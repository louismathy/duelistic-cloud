package com.duelistic.commands;

import com.duelistic.Cloud;
import com.duelistic.ui.ConsoleUi;
import com.duelistic.util.ResourceUtil;
import com.duelistic.util.VirtualResourceUtil;

/**
 * Displays CPU and RAM usage
 */
public class ResourcesCommand implements Command{
    @Override
    public String getName() {
        return "resources";
    }

    /**
     * Prints the formatted RAM and CPU usage
     */
    @Override
    public void execute(String[] args) {
        ConsoleUi.info("RAM: " + getFormattedRamUsage());
        ConsoleUi.info("CPU: (System based) " + ResourceUtil.getCPUUsage() + "%");
    }


    /**
     * Returns a formatted String of the ram usage vs maximum ram usage
     */
    private String getFormattedRamUsage() {
        long total;
        long free;
        long used;
        String prefix;
        if (Cloud.getInstance().getCloudConfig().isBasedOnOverallSystemMemory()) {
            total = ResourceUtil.getTotalMemory();
            free = ResourceUtil.getFreeMemory();
            used = ResourceUtil.getUsedMemory();
            prefix = "(System based) ";
        } else {
            VirtualResourceUtil resourceUtil = Cloud.getInstance().getVirtualResourceUtil();
            total = resourceUtil.getTotalMaxRam();
            free = resourceUtil.getFreeRam();
            used = resourceUtil.getTotalRamDemandMb();
            prefix = "(Virtual based) ";
        }
        return prefix
                + (used)
                + "/" + total
                + " MiB (" + free
                + " MiB free)";
    }

    @Override
    public String getUsage() {
        return "resources";
    }
}
