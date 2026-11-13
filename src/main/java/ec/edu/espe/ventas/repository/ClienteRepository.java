package ec.edu.espe.ventas.repository;

import ec.edu.espe.ventas.entity.Cliente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de clientes.
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCedula(String cedula);

    Optional<Cliente> findByEmail(String email);

    /** Unicidad del correo sin distinguir mayúsculas, incluyendo datos anteriores. */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCedula(String cedula);

    boolean existsByEmail(String email);

    /**
     * Búsqueda por apellido, sin distinguir mayúsculas ni minúsculas.
     */
    List<Cliente> findByApellidoContainingIgnoreCase(String apellido);

    List<Cliente> findByActivoTrue();

    long countByActivoTrue();
}
