package ca.bc.gov.educ.penreg.api.filter;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * @author om
 */
@Service
public class Converters {

	private final Map<Class<?>, Function<String, ? extends Comparable<?>>> map = new HashMap<>();

	@PostConstruct
	public void init() {
		map.put(String.class, s -> s);
		map.put(Long.class, Long::valueOf);
		map.put(Integer.class, Integer::valueOf);
		map.put(ChronoLocalDate.class, LocalDate::parse);
		map.put(ChronoLocalDateTime.class, LocalDateTime::parse);
		map.put(UUID.class,UUID::fromString);
	}

	@SuppressWarnings("unchecked")
	public <T extends Comparable<T>> Function<String, T> getFunction(Class<?> classObj) {
		return (Function<String, T>) map.get(classObj);
	}

}
