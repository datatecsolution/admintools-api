package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.ReportSchedule;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ReportScheduleCRUD extends CrudRepository<ReportSchedule, Integer> {
    List<ReportSchedule> findAllByOrderByIdAsc();
    List<ReportSchedule> findByActivoTrue();
}
