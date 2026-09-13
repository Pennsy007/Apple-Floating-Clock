package com.appleclock.floating.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工业级高精度网络授时校准管理器
 * - 双引擎：电商毫秒级 API (淘宝/拼多多/苏宁) + NTP 原子授时 (阿里云 NTP)
 * - 毫秒精度采集：淘汰秒级 HTTP 标头，百分之百采集 13 位毫秒时间戳
 * - 最小 RTT 滤波算法：剔除网络排队抖动，选取往返时延极低且对称的样本
 * - 确保一次点击即精准对齐网页北京时间
 */
public class NetworkTimeManager {

    private static final String TAG = "NetworkTimeManager";
    private static volatile NetworkTimeManager instance;

    public interface SyncCallback {
        void onSuccess(long offsetMs, long rttMs);
        void onError(String message);
    }

    public interface OnTimeCalibratedListener {
        void onCalibrated(long offsetMs);
    }

    public static NetworkTimeManager getInstance(Context context) {
        if (instance == null) {
            synchronized (NetworkTimeManager.class) {
                if (instance == null) {
                    instance = new NetworkTimeManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private final PreferenceManager prefManager;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<OnTimeCalibratedListener> listeners = new CopyOnWriteArrayList<>();

    private volatile long networkOffset = 0L;
    private volatile long userCompensation = 0L;
    private volatile boolean isCalibrated = false;

    private NetworkTimeManager(Context context) {
        this.prefManager = new PreferenceManager(context);
        this.networkOffset = prefManager.getNetworkOffset();
        this.userCompensation = prefManager.getUserCompensation();
        this.isCalibrated = prefManager.isCalibrated();
    }

    public void addListener(OnTimeCalibratedListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(OnTimeCalibratedListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * 获取经过网络校准与用户补偿后的精准毫秒时间戳
     */
    public long getCalibratedTimeMillis() {
        return System.currentTimeMillis() + networkOffset + userCompensation;
    }

    public long getNetworkOffset() {
        return networkOffset;
    }

    public long getUserCompensation() {
        return userCompensation;
    }

    public boolean isCalibrated() {
        return isCalibrated;
    }

    public void setUserCompensation(long compensationMs) {
        this.userCompensation = compensationMs;
        prefManager.saveUserCompensation(compensationMs);
        notifyListeners(networkOffset + compensationMs);
    }

    private static class TimeSample implements Comparable<TimeSample> {
        final String source;
        final long rtt;
        final long offset;

        TimeSample(String source, long rtt, long offset) {
            this.source = source;
            this.rtt = rtt;
            this.offset = offset;
        }

        @Override
        public int compareTo(TimeSample o) {
            return Long.compare(this.rtt, o.rtt);
        }

        @Override
        public String toString() {
            return source + "[rtt=" + rtt + "ms, offset=" + offset + "ms]";
        }
    }

    /**
     * 异步触发高精度网络时间校准（一次即对齐）
     */
    public void syncNetworkTime(SyncCallback callback) {
        executor.execute(() -> {
            List<TimeSample> samples = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(4);

            // 1. 拼多多毫秒接口 (网络链路极短，国内 CDN 延迟常在 20-50ms)
            executor.execute(() -> {
                try {
                    // 先做一次预热，再取一次低延迟样本
                    queryPddTime();
                    TimeSample s = queryPddTime();
                    if (s != null) samples.add(s);
                } catch (Exception ignored) {}
                latch.countDown();
            });

            // 2. 淘宝/阿里毫秒接口 (阿里官方标准电商授时，秒杀基准)
            executor.execute(() -> {
                try {
                    queryTaobaoTime();
                    TimeSample s = queryTaobaoTime();
                    if (s != null) samples.add(s);
                } catch (Exception ignored) {}
                latch.countDown();
            });

            // 3. 苏宁毫秒接口
            executor.execute(() -> {
                try {
                    TimeSample s = querySuningTime();
                    if (s != null) samples.add(s);
                } catch (Exception ignored) {}
                latch.countDown();
            });

            // 4. 阿里云标准 NTP 授时 (UDP 123 端口，纯微秒级原子钟)
            executor.execute(() -> {
                try {
                    TimeSample s = querySntpTime("ntp.aliyun.com");
                    if (s != null) samples.add(s);
                } catch (Exception ignored) {}
                latch.countDown();
            });

            try {
                // 等待各节点返回，最多 1800ms，保证用户界面极速响应
                latch.await(1800, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {}

            boolean finalSuccess = false;
            long bestOffset = 0;
            long bestRtt = 0;
            String errorMsg = "网络连接超时，请检查网络";

            if (!samples.isEmpty()) {
                // 按 RTT 升序排列，选取网络延迟最低、对称性最高的样本
                Collections.sort(samples);
                Log.i(TAG, "All calibrated time samples: " + samples);

                // 筛选低延迟样本（优先 RTT < 200ms）
                List<TimeSample> goodSamples = new ArrayList<>();
                for (TimeSample s : samples) {
                    if (s.rtt < 300) {
                        goodSamples.add(s);
                    }
                }
                if (goodSamples.isEmpty()) {
                    goodSamples.addAll(samples);
                }

                // 取前 3 个最优样本的加权平均 / 中位数，抵消微小不对称
                if (goodSamples.size() == 1) {
                    bestOffset = goodSamples.get(0).offset;
                    bestRtt = goodSamples.get(0).rtt;
                } else if (goodSamples.size() == 2) {
                    bestOffset = (goodSamples.get(0).offset + goodSamples.get(1).offset) / 2;
                    bestRtt = goodSamples.get(0).rtt;
                } else {
                    // 取前 3 个中位数
                    long o1 = goodSamples.get(0).offset;
                    long o2 = goodSamples.get(1).offset;
                    long o3 = goodSamples.get(2).offset;
                    bestOffset = median(o1, o2, o3);
                    bestRtt = goodSamples.get(0).rtt;
                }

                finalSuccess = true;
                networkOffset = bestOffset;
                isCalibrated = true;
                prefManager.saveNetworkOffset(bestOffset);
                prefManager.saveCalibrated(true);

                Log.i(TAG, "Best calibration result: offset=" + bestOffset + "ms, rtt=" + bestRtt + "ms");
                notifyListeners(bestOffset);
            }

            final boolean success = finalSuccess;
            final long fOffset = bestOffset;
            final long fRtt = bestRtt;
            final String fError = errorMsg;

            mainHandler.post(() -> {
                if (callback != null) {
                    if (success) {
                        callback.onSuccess(fOffset, fRtt);
                    } else {
                        callback.onError(fError);
                    }
                }
            });
        });
    }

    private void notifyListeners(long offset) {
        mainHandler.post(() -> {
            for (OnTimeCalibratedListener l : listeners) {
                try {
                    l.onCalibrated(offset);
                } catch (Exception ignored) {}
            }
        });
    }

    private long median(long a, long b, long c) {
        if ((a >= b && a <= c) || (a <= b && a >= c)) return a;
        if ((b >= a && b <= c) || (b <= a && b >= c)) return b;
        return c;
    }

    /**
     * 拼多多毫秒授时
     */
    private TimeSample queryPddTime() {
        try {
            long t0 = System.currentTimeMillis();
            URL url = new URL("http://api.pinduoduo.com/api/server/_stm");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1200);
            conn.setReadTimeout(1200);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String resp = reader.readLine();
                reader.close();
                long t1 = System.currentTimeMillis();
                long rtt = t1 - t0;

                Pattern pattern = Pattern.compile("\"server_time\":\\s*(\\d{13})");
                Matcher matcher = pattern.matcher(resp);
                if (matcher.find()) {
                    long serverMs = Long.parseLong(matcher.group(1));
                    long localMid = (t0 + t1) / 2;
                    long offset = serverMs - localMid;
                    return new TimeSample("PDD", rtt, offset);
                }
            }
            conn.disconnect();
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 淘宝移动端毫秒授时
     */
    private TimeSample queryTaobaoTime() {
        try {
            long t0 = System.currentTimeMillis();
            URL url = new URL("http://acs.m.taobao.com/gw/mtop.common.getTimestamp/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1200);
            conn.setReadTimeout(1200);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String resp = reader.readLine();
                reader.close();
                long t1 = System.currentTimeMillis();
                long rtt = t1 - t0;

                Pattern pattern = Pattern.compile("\"t\":\\s*\"?(\\d{13})\"?");
                Matcher matcher = pattern.matcher(resp);
                if (matcher.find()) {
                    long serverMs = Long.parseLong(matcher.group(1));
                    long localMid = (t0 + t1) / 2;
                    long offset = serverMs - localMid;
                    return new TimeSample("Taobao", rtt, offset);
                }
            }
            conn.disconnect();
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 苏宁易购毫秒授时
     */
    private TimeSample querySuningTime() {
        try {
            long t0 = System.currentTimeMillis();
            URL url = new URL("https://f.m.suning.com/api/ct.do");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1200);
            conn.setReadTimeout(1200);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String resp = reader.readLine();
                reader.close();
                long t1 = System.currentTimeMillis();
                long rtt = t1 - t0;

                Pattern pattern = Pattern.compile("\"currentTime\":\\s*(\\d{13})");
                Matcher matcher = pattern.matcher(resp);
                if (matcher.find()) {
                    long serverMs = Long.parseLong(matcher.group(1));
                    long localMid = (t0 + t1) / 2;
                    long offset = serverMs - localMid;
                    return new TimeSample("Suning", rtt, offset);
                }
            }
            conn.disconnect();
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 原生 UDP SNTP (NTP) 协议客户端实现
     * 向 ntp.aliyun.com 请求 48 字节时间戳报文
     */
    private TimeSample querySntpTime(String host) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(1200);
            InetAddress address = InetAddress.getByName(host);

            byte[] buffer = new byte[48];
            // LI = 0, VN = 3 (NTP v3), Mode = 3 (Client) -> 0x1B
            buffer[0] = 0x1B;

            long t0 = System.currentTimeMillis();
            writeTimeStamp(buffer, 40, t0);

            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, 123);
            socket.send(request);

            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            long t3 = System.currentTimeMillis();

            long t1 = readTimeStamp(buffer, 32); // 接收时间
            long t2 = readTimeStamp(buffer, 40); // 发送时间

            long rtt = (t3 - t0) - (t2 - t1);
            long offset = ((t1 - t0) + (t2 - t3)) / 2;

            if (t1 > 0 && t2 > 0 && rtt >= 0 && rtt < 400) {
                return new TimeSample("NTP(" + host + ")", rtt, offset);
            }
        } catch (Exception ignored) {
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
        return null;
    }

    private static final long OFFSET_1900_TO_1970 = 2208988800L;

    private void writeTimeStamp(byte[] buffer, int offset, long time) {
        long seconds = time / 1000L;
        long milliseconds = time - seconds * 1000L;
        seconds += OFFSET_1900_TO_1970;

        buffer[offset++] = (byte) (seconds >> 24);
        buffer[offset++] = (byte) (seconds >> 16);
        buffer[offset++] = (byte) (seconds >> 8);
        buffer[offset++] = (byte) (seconds >> 0);

        long fraction = milliseconds * 0x100000000L / 1000L;
        buffer[offset++] = (byte) (fraction >> 24);
        buffer[offset++] = (byte) (fraction >> 16);
        buffer[offset++] = (byte) (fraction >> 8);
        buffer[offset] = (byte) (fraction >> 0);
    }

    private long readTimeStamp(byte[] buffer, int offset) {
        long seconds = ((long) (buffer[offset] & 0xFF) << 24)
                | ((long) (buffer[offset + 1] & 0xFF) << 16)
                | ((long) (buffer[offset + 2] & 0xFF) << 8)
                | ((long) (buffer[offset + 3] & 0xFF));

        long fraction = ((long) (buffer[offset + 4] & 0xFF) << 24)
                | ((long) (buffer[offset + 5] & 0xFF) << 16)
                | ((long) (buffer[offset + 6] & 0xFF) << 8)
                | ((long) (buffer[offset + 7] & 0xFF));

        long ms = (fraction * 1000L) / 0x100000000L;
        return ((seconds - OFFSET_1900_TO_1970) * 1000L) + ms;
    }
}
