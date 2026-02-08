package com.simra.konsumgandalf.common.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.simra.konsumgandalf.common.models.enums.BikeType;
import com.simra.konsumgandalf.common.models.enums.IncidentType;
import com.simra.konsumgandalf.common.models.enums.ParticipantType;
import com.simra.konsumgandalf.common.models.enums.PhoneLocation;
import com.simra.konsumgandalf.common.utils.converter.EnumConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.geolatte.geom.Point;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(indexes = { @Index(columnList = "planet_osm_line_osm_id"), @Index(name = "idx_way_gist", columnList = "way") })
@JsonIgnoreProperties({ "i1", "i2", "i3", "i4", "i5", "i6", "i7", "i8", "i9", "i10", "ts" })
public class RideIncident extends TimeBaseClass {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "planet_osm_line_osm_id")
	@JsonIgnore
	private PlanetOsmLine planetOsmLine;

	@ManyToOne(fetch = FetchType.LAZY)
	@JsonIgnore
	private RideEntity rideEntity;

	@CsvBindByName(column = "lat")
	private double lat;

	@CsvBindByName(column = "lon")
	private double lng;

	@CsvCustomBindByName(column = "bike", converter = EnumConverter.class)
	@Column(length = 20)
	@Enumerated(EnumType.STRING)
	private BikeType bike;

	@CsvBindByName(column = "childCheckBox")
	private boolean childCheckBox;

	@CsvBindByName(column = "trailerCheckBox")
	private boolean trailerCheckBox;

	@CsvCustomBindByName(column = "pLoc", converter = EnumConverter.class)
	@Column(length = 16)
	@Enumerated(EnumType.STRING)
	private PhoneLocation phoneLocation = PhoneLocation.OTHER;

	@CsvCustomBindByName(column = "incident", converter = EnumConverter.class)
	@Column(length = 20)
	@Enumerated(EnumType.STRING)
	private IncidentType incidentType = IncidentType.NOTHING;

	@CsvBindByName(column = "desc")
	@Column(columnDefinition = "text")
	private String description;

	@CsvBindByName(column = "scary")
	private boolean scary;

	@ElementCollection(fetch = FetchType.EAGER)
	@Enumerated(EnumType.STRING)
	@Column(length = 16)
	private List<ParticipantType> participantsInvolved = new ArrayList<>();

	@CsvBindByName(column = "ts")
	@Transient
	private long ts;

	@Temporal(TemporalType.TIMESTAMP)
	private java.util.Date timeStamp;

	@JsonIgnore
	@Column(columnDefinition = "geometry(Point,4326)")
	private Point way;

	public RideIncident() {
	}

	public RideIncident(double lat, double lng, long ts, BikeType bike, boolean childCheckBox, boolean trailerCheckBox,
			PhoneLocation phoneLocation, IncidentType incidentType, String description, boolean scary) {
		this.lat = lat;
		this.lng = lng;
		this.ts = ts;
		this.bike = bike;
		this.childCheckBox = childCheckBox;
		this.trailerCheckBox = trailerCheckBox;
		this.phoneLocation = phoneLocation;
		this.incidentType = incidentType;
		this.description = description;
		this.scary = scary;
	}

    public void addParticipantsInvolved(ParticipantType participantInvolved) {
        this.participantsInvolved.add(participantInvolved);
    }

	/**
	 * The following attributes pollute the entity with unnecessary information therefore
	 * they are not saved
	 */
	@Transient
	@CsvBindByName(column = "i1")
	private int i1;

	@Transient
	@CsvBindByName(column = "i2")
	private int i2;

	@Transient
	@CsvBindByName(column = "i3")
	private int i3;

	@Transient
	@CsvBindByName(column = "i4")
	private int i4;

	@Transient
	@CsvBindByName(column = "i5")
	private int i5;

	@Transient
	@CsvBindByName(column = "i6")
	private int i6;

	@Transient
	@CsvBindByName(column = "i7")
	private int i7;

	@Transient
	@CsvBindByName(column = "i8")
	private int i8;

	@Transient
	@CsvBindByName(column = "i9")
	private int i9;

	@Transient
	@CsvBindByName(column = "i10")
	private int i10;
}
