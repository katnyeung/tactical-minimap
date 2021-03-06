package org.tactical.minimap.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tactical.minimap.repository.MarkerResponse;

public interface MarkerResponseDAO<T extends MarkerResponse> extends JpaRepository<T, Long> {

}
