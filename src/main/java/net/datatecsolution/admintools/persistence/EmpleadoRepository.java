package net.datatecsolution.admintools.persistence;

import net.datatecsolution.admintools.domain.Seller;
import net.datatecsolution.admintools.domain.repository.SellerRepository;
import net.datatecsolution.admintools.persistence.crud.EmpleadoCRUD;
import net.datatecsolution.admintools.persistence.entity.Empleado;
import net.datatecsolution.admintools.persistence.mapper.SellerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class EmpleadoRepository implements SellerRepository {
    @Autowired
    private EmpleadoCRUD empleadoCRUD;

    @Autowired
    private SellerMapper sellerMapper;

    @Override
    public Optional<Seller> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public Optional<Seller> findByUser(String user) {
        Optional<Empleado> empleado=empleadoCRUD.findByUsuario(user);
        return Optional.of(sellerMapper.toSeller(empleado.get()));
    }
}
