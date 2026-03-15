package com.simra.konsumgandalf.rides.services;

import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import com.simra.konsumgandalf.common.models.entities.*;
import com.simra.konsumgandalf.rides.repositories.IntersectionBaseRepository;
import com.simra.konsumgandalf.rides.repositories.MatchedPointRepository;
import com.simra.konsumgandalf.rides.repositories.RidePointRepository;
import com.simra.konsumgandalf.rides.repositories.RideRepository;
import jakarta.transaction.Transactional;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class RidePersistenceService {

	private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

	@Autowired
	private RideRepository rideRepository;

	@Autowired
	private RidePointRepository ridePointRepository;

	@Autowired
	private MatchedPointRepository matchedPointRepository;

	@Autowired
	private IntersectionBaseRepository intersectionBaseRepository;

	RidePersistenceService() {
	}

	@Transactional
	public void saveRide(Ride ride) {
		rideRepository.save(ride);
	}

	@Transactional
	public void saveMatchedPoints(List<MatchedPoint> matchedPoints) {
		matchedPointRepository.saveAll(matchedPoints);
	}

	@Transactional
	protected void saveIntersectionsLists(List<IntersectionNode> intersectionNodeList,
			List<IntersectionEdge> intersectionEdgeList) {
		List<IntersectionBase> all = new ArrayList<>();
		all.addAll(intersectionEdgeList);
		all.addAll(intersectionNodeList);

		if (all.isEmpty()) {
			return;
		}

		for (int i = 0; i < all.size(); i++) {
			all.get(i).setIndexInRide(i);
		}

		List<Integer> prevIndexInRide = new ArrayList<>();
		for (IntersectionBase element : all) {
			prevIndexInRide
				.add(element.getPrevIntersection() != null ? element.getPrevIntersection().getIndexInRide() : null);
			element.setPrevIntersection(null);
		}

		intersectionBaseRepository.saveAll(all);

		for (int i = 0; i < all.size(); i++) {
			IntersectionBase current = all.get(i);
			IntersectionBase prev = prevIndexInRide.get(i) != null ? all.get(prevIndexInRide.get(i)) : null;

			current.setPrevIntersection(prev);
			if (prev != null) {
				prev.setNextIntersection(current);
			}
		}

		intersectionBaseRepository.flush();
	}

	@Transactional
	public void saveRidePointsAndSetRideIds(Ride ride) {
		List<RidePoint> ridePointList = new ArrayList<>();
		for (MatchInformation loc : ride.getCoordinates()) {
			RidePoint p = new RidePoint();
			p.setRide(ride);
			p.setTimestamp(new Date(loc.getTimestamp() * 1000));
			Coordinate coord = new Coordinate(loc.getLng(), loc.getLat());
			p.setGeom(geometryFactory.createPoint(coord));
			ridePointList.add(p);
		}
		ridePointRepository.saveAll(ridePointList);

		for (int i = 0; i < ridePointList.size(); i++) {
			ride.getCoordinates().get(i).setRidePointId(ridePointList.get(i).getId());
		}
	}

}
