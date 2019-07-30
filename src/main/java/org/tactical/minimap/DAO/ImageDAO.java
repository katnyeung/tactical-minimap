package org.tactical.minimap.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tactical.minimap.repository.Image;

public interface ImageDAO extends JpaRepository<Image, Long> {
	

}
