package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.ReportSendLog;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReportSendLogCRUD extends CrudRepository<ReportSendLog, Integer> {
    boolean existsByScheduleIdAndFechaProgramadaAndOrigen(int scheduleId, LocalDate fecha, String origen);
    List<ReportSendLog> findTop30ByScheduleIdOrderByIdDesc(int scheduleId);
}
