package com.wefood.back.product.repository;

import com.wefood.back.product.entity.Item;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * class: ItemRepository.
 *
 * @author JBumLee
 * @version 2024/08/20
 */
public interface ItemRepository extends JpaRepository<Item,Long> {

    Optional<Item> findByName(String name);
}
