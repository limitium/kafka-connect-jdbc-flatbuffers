package org.example.dao.generated;

import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.example.dao.FlatbuffersDAO;
import org.example.models.FBReport;

/**
 * Used to bind generics
 */
public class ReportDAO extends FlatbuffersDAO<FBReport, ReportWrapper> {
    public ReportDAO(ProcessorContext<?, ?> context, String storeName) {
        super(context, storeName, ReportDAO.class);
    }
}
