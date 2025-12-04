package com.simra.konsumgandalf.profiles.services;

import com.simra.konsumgandalf.common.models.entities.Profile;
import com.simra.konsumgandalf.common.models.entities.SimraRegion;
import com.simra.konsumgandalf.common.models.maps.SimraRegionEnumNameMapper;
import com.simra.konsumgandalf.common.utils.ScoreUtils;
import com.simra.konsumgandalf.common.utils.services.CsvUtilService;
import com.simra.konsumgandalf.common.utils.services.FileReaderService;
import com.simra.konsumgandalf.profiles.repositories.ProfileRepository;
import com.simra.konsumgandalf.profiles.repositories.ProfileSimraRegionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class ProfileService {

	private static Path dataPath;

	@Autowired
	private FileReaderService fileReaderService;

	@Autowired
	private ProfileRepository profileRepository;

	@Autowired
	private CsvUtilService csvUtilService;

	@Autowired
	private ProfileSimraRegionRepository profileSimraRegionRepository;

	@Autowired
	private BloomFilterProfileExistenceChecker bloomFilterProfileExistenceChecker;

	private static final Logger _logger = LoggerFactory.getLogger(ProfileService.class);

	private static final SimraRegionEnumNameMapper simraRegionEnumNameMapper = new SimraRegionEnumNameMapper();

	ProfileService(@Value("${SIMRA_PROFILE_FILE_PATH:./}") Path filePath) {
		dataPath = filePath;
	}

	public void loadAllPrevProfiles() {
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		AtomicInteger counter = new AtomicInteger(0);

		try {
			Files.walk(dataPath, 8, FileVisitOption.FOLLOW_LINKS)
				.filter(Files::isRegularFile)
				.filter(FileReaderService::isEntityFile)
				.map(Path::toString)
				.filter(bloomFilterProfileExistenceChecker::shouldBeProcessed)
				.forEach(path -> {
					CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
						try {
							// Problem
							_logger.info("Processing file: " + path.toString() + " on thread: "
									+ Thread.currentThread().getName());
							Optional<Profile> profile = generateNewProfileEntity(path);
							bloomFilterProfileExistenceChecker.add(path);
							if (profile.isPresent()) {
								counter.incrementAndGet();
							}
						}
						catch (Exception e) {
							_logger.error("Error processing file: " + path.toString(), e);
						}
					});
					futures.add(future);
				});
		}
		catch (Exception e) {
			_logger.error("Error loading profiles", e);
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		_logger.info("Loaded {} new profiles", counter.get());
	}

	public long count() {
		return profileRepository.count();
	}

	protected Optional<Profile> generateProfileFromCsv(String path) {
		String fileContent = fileReaderService.readFileFromPath(path);

		fileContent = Arrays.stream(fileContent.split("\n"))
			.filter(line -> !line.contains("#"))
			.reduce((line1, line2) -> line1 + "\n" + line2)
			.orElse("");

		Optional<Profile> optionalProfile = csvUtilService.parseCsvToSingleModel(fileContent, Profile.class);
		if (optionalProfile.isEmpty()) {
			_logger.error("Could not parse profile from file: {}", path);
			return Optional.empty();
		}

		Profile profile = optionalProfile.get();
		if (profile.getNumberOfRides() <= 0 || profile.getNumberOfRides() > 5000) {
			_logger.error("Invalid number of rides in profile: {}", profile);
			return Optional.empty();
		}

		float dangerousScore = ScoreUtils.calculateDangerousScore(profile.getNumberOfRides(),
				profile.getNumberOfIncidents(), profile.getNumberOfScaryIncidents());
		if (dangerousScore >= 5) {
			_logger.error("Invalid dangerous score calculated for profile: {}", profile);
			return Optional.empty();
		}

		profile.setLastModified(fileReaderService.getFileLastModified(path));
		profile.setPath(path);

		Optional<String> regionName = simraRegionEnumNameMapper.getNameForEnum(profile.getSimraRegionGroup());
		if (regionName.isEmpty()) {
			_logger.error("Could not find region name for enum: " + profile.getSimraRegionGroup());
		}
		else {
			SimraRegion simraRegion = profileSimraRegionRepository.findByName(regionName.get()).orElseGet(() -> {
				return new SimraRegion(regionName.get());
			});
			profile.setSimraRegion(simraRegion);
		}

		return Optional.of(profile);
	}

	private Optional<Profile> generateNewProfileEntity(String path) {
		Optional<Profile> profile = generateProfileFromCsv(path);

		profile.ifPresent(value -> profileRepository.save(value));
		return profile;
	}

}
