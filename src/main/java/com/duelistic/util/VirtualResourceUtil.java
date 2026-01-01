package com.duelistic.util;

import com.duelistic.Cloud;
import com.duelistic.system.CloudDirectories;
import com.duelistic.system.ServerStatus;
import com.duelistic.system.ServerStatusService;
import com.duelistic.system.TemplateConfig;
import com.duelistic.ui.ConsoleUi;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

/**
 * Utility class that uses virtual ram limits and possible max cases to handle resources
 */
public class VirtualResourceUtil {
    private long totalMaxRam;
    private final ServerStatusService statusService;
    private final CloudDirectories directories;

    public VirtualResourceUtil(long totalMaxRam, ServerStatusService statusService, CloudDirectories directories) {
        this.totalMaxRam = totalMaxRam;
        this.statusService = statusService;
        this.directories = directories;
    }

    /**
     * Returns the sum of the specified ram of the servers + the current ram usage of the cloud
     * @return memory in MiB
     */
    public long getTotalRamDemandMb() {
        try {
            long usedMemory = 0;
            for (ServerStatus status : statusService.listStatuses()) {
                if (!directories.templateExists(status.getTemplate())) {
                    throw new IOException("Template "+status.getTemplate()+ " doesn't exist");
                }
                TemplateConfig config = TemplateConfig.loadFrom(directories.getTemplateConfigFile(status.getTemplate()));
                usedMemory += config.getMaxRamMb();
            }
            MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            MemoryUsage nonHeapMemoryUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
            usedMemory += heapMemoryUsage.getUsed() / 1024L / 1024L;
            usedMemory += nonHeapMemoryUsage.getUsed() / 1024L / 1024L;
            return usedMemory;
        } catch (IOException e) {
            ConsoleUi.error("Error getting Used Ram: " + e.getMessage());
        }
        return -1;
    }

    public long getFreeRam() {
        return getTotalMaxRam() - getTotalRamDemandMb();
    }

    public long getTotalMaxRam() {
        return totalMaxRam;
    }

    public void setTotalMaxRam(long totalMaxRam) {
        this.totalMaxRam = totalMaxRam;
    }
}
