package org.tactical.minimap.repository;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue(value = "up")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class UpResponse extends MarkerResponse {

}