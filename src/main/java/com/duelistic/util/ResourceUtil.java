package com.duelistic.util;

import com.sun.management.OperatingSystemMXBean;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.lang.management.ManagementFactory;

/**
 * Utility class that uses oshi and sun to return system monitoring
 */
public class ResourceUtil {
    private static OperatingSystemMXBean mxBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static SystemInfo systemInfo = new SystemInfo();
    private static HardwareAbstractionLayer abstractionLayer = systemInfo.getHardware();
    private static GlobalMemory memory = abstractionLayer.getMemory();

    /**
     * Returns the current CPU usage in %
     */
    public static double getCPUUsage() {
        return (double) Math.round(mxBean.getSystemCpuLoad() * 100 * 100) / 100;
    }

    /**
     * Returns the total memory size in MiB
     */
    public static long getTotalMemory() {
        return memory.getTotal() / 1024 / 1024;
    }

    /**
     * Returns the free memory size in Mib
     */
    public static long getFreeMemory() {
        return memory.getAvailable() / 1024 / 1024;
    }
    /**
     * Returns the used memory size in Mib
     */
    public static long getUsedMemory() {
        return getTotalMemory() - getFreeMemory();
    }
}
