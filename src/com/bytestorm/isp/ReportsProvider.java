package com.bytestorm.isp;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public interface ReportsProvider {
    public File[] getEarningsReportsFiles() throws IOException, IllegalArgumentException;
    public File[] getSalesReportsFiles() throws IOException, IllegalArgumentException;
    public Date getDate();
}
