package com.pharmaprice.report.dto;

import com.pharmaprice.report.domain.FlagReason;
import com.pharmaprice.report.domain.ReportStatus;

public record AdminPriceReportUpdateRequest(ReportStatus status, Boolean flagged, FlagReason reason) {
}
