package com.mawen.search.core.convert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.mawen.search.core.annotation.DateFormat;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.annotation.ValueConverter;
import com.mawen.search.core.document.Document;
import com.mawen.search.core.document.MapDocument;
import com.mawen.search.core.domain.Criteria;
import com.mawen.search.core.domain.Range;
import com.mawen.search.core.domain.SeqNoPrimaryTerm;
import com.mawen.search.core.mapping.PropertyValueConverter;
import com.mawen.search.core.mapping.SimpleElasticsearchMappingContext;
import com.mawen.search.core.query.CriteriaQuery;
import com.mawen.search.core.query.Query;
import org.intellij.lang.annotations.Language;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;

public class MappingElasticsearchConverterUnitTests {

	static final String JSON_STRING = "{\"name\":\"Grat\",\"model\":\"Ford\"}";
	static final String CAR_MODEL = "Ford";
	static final String CAR_NAME = "Grat";
	MappingElasticsearchConverter mappingElasticsearchConverter;

	Person sarahConnor;
	Person kyleReese;
	Person t800;

	Inventory gun = new Gun("Glock 19", 33);
	Inventory grenade = new Grenade("40 mm");
	Inventory rifle = new Rifle("AR-18 Assault Rifle", 3.17, 40);
	Inventory shotGun = new ShotGun("Ithaca 37 Pump Shotgun");

	Address observatoryRoad;
	Place bigBunsCafe;

	Document sarahAsMap;
	Document t800AsMap;
	Document kyleAsMap;
	Document gratiotAveAsMap;
	Document locationAsMap;
	Document gunAsMap;
	Document grenadeAsMap;
	Document rifleAsMap;
	Document shotGunAsMap;
	Document bigBunsCafeAsMap;
	Document notificationAsMap;

	@BeforeEach
	public void init() {

		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setInitialEntitySet(Collections.singleton(Rifle.class));
		mappingContext.afterPropertiesSet();

		mappingElasticsearchConverter = new MappingElasticsearchConverter(mappingContext, new GenericConversionService());
		mappingElasticsearchConverter.setConversions(
				new ElasticsearchCustomConversions(Arrays.asList(new ShotGunToMapConverter(), new MapToShotGunConverter())));
		mappingElasticsearchConverter.afterPropertiesSet();

		sarahConnor = new Person();
		sarahConnor.id = "sarah";
		sarahConnor.name = "Sarah Connor";
		sarahConnor.gender = Gender.MAN;

		kyleReese = new Person();
		kyleReese.id = "kyle";
		kyleReese.gender = Gender.MAN;
		kyleReese.name = "Kyle Reese";

		t800 = new Person();
		t800.id = "t800";
		t800.name = "T-800";
		t800.gender = Gender.MACHINE;

		t800AsMap = Document.create();
		t800AsMap.put("id", "t800");
		t800AsMap.put("name", "T-800");
		t800AsMap.put("gender", "MACHINE");

		observatoryRoad = new Address();
		observatoryRoad.city = "Los Angeles";
		observatoryRoad.street = "2800 East Observatory Road";
		observatoryRoad.location = new Point(-118.3026284D, 34.118347D);

		bigBunsCafe = new Place();
		bigBunsCafe.setName("Big Buns Cafe");
		bigBunsCafe.setCity("Los Angeles");
		bigBunsCafe.setStreet("15 South Fremont Avenue");
		bigBunsCafe.setLocation(new Point(-118.1545845D, 34.0945637D));

		sarahAsMap = Document.create();
		sarahAsMap.put("id", "sarah");
		sarahAsMap.put("name", "Sarah Connor");
		sarahAsMap.put("gender", "MAN");

		kyleAsMap = Document.create();
		kyleAsMap.put("id", "kyle");
		kyleAsMap.put("gender", "MAN");
		kyleAsMap.put("name", "Kyle Reese");

		locationAsMap = Document.create();
		locationAsMap.put("y", 34.118347D);
		locationAsMap.put("x", -118.3026284D);

		gratiotAveAsMap = Document.create();
		gratiotAveAsMap.put("city", "Los Angeles");
		gratiotAveAsMap.put("street", "2800 East Observatory Road");
		gratiotAveAsMap.put("location", locationAsMap);

		bigBunsCafeAsMap = Document.create();
		bigBunsCafeAsMap.put("name", "Big Buns Cafe");
		bigBunsCafeAsMap.put("city", "Los Angeles");
		bigBunsCafeAsMap.put("street", "15 South Fremont Avenue");
		bigBunsCafeAsMap.put("location", new LinkedHashMap<>());
		((HashMap<String, Object>) bigBunsCafeAsMap.get("location")).put("y", 34.0945637D);
		((HashMap<String, Object>) bigBunsCafeAsMap.get("location")).put("x", -118.1545845D);

		gunAsMap = Document.create();
		gunAsMap.put("label", "Glock 19");
		gunAsMap.put("shotsPerMagazine", 33);

		grenadeAsMap = Document.create();
		grenadeAsMap.put("label", "40 mm");

		rifleAsMap = Document.create();
		rifleAsMap.put("label", "AR-18 Assault Rifle");
		rifleAsMap.put("weight", 3.17D);
		rifleAsMap.put("maxShotsPerMagazine", 40);

		shotGunAsMap = Document.create();
		shotGunAsMap.put("model", "Ithaca 37 Pump Shotgun");

		notificationAsMap = Document.create();
		notificationAsMap.put("id", 1L);
		notificationAsMap.put("fromEmail", "from@email.com");
		notificationAsMap.put("toEmail", "to@email.com");
		Map<String, Object> data = new HashMap<>();
		data.put("documentType", "abc");
		data.put("content", null);
		notificationAsMap.put("params", data);
	}

	@Test
	public void shouldFailToInitializeGivenMappingContextIsNull() {

		// given
		assertThatThrownBy(() -> new MappingElasticsearchConverter(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void shouldReturnMappingContextWithWhichItWasInitialized() {

		// given
		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);

		// then
		assertThat(converter.getMappingContext()).isNotNull();
		assertThat(converter.getMappingContext()).isSameAs(mappingContext);
	}

	@Test
	public void shouldReturnDefaultConversionService() {

		// given
		MappingElasticsearchConverter converter = new MappingElasticsearchConverter(
				new SimpleElasticsearchMappingContext());

		// when
		ConversionService conversionService = converter.getConversionService();

		// then
		assertThat(conversionService).isNotNull();
	}

	@Test
	public void shouldMapObjectToJsonString() {
		Car car = new Car();
		car.setModel(CAR_MODEL);
		car.setName(CAR_NAME);
		String jsonResult = mappingElasticsearchConverter.mapObject(car).toJson();

		assertThat(jsonResult).isEqualTo(JSON_STRING);
	}

	@Test 
	public void shouldReadJsonStringToObject() {
		// Given

		// When
		Car result = mappingElasticsearchConverter.read(Car.class, Document.parse(JSON_STRING));

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo(CAR_NAME);
		assertThat(result.getModel()).isEqualTo(CAR_MODEL);
	}

	@Test 
	public void ignoresReadOnlyProperties() {

		// given
		Sample sample = new Sample();
		sample.setReadOnly("readOnly");
		sample.setProperty("property");
		sample.setJavaTransientProperty("javaTransient");
		sample.setAnnotatedTransientProperty("transient");

		// when
		String result = mappingElasticsearchConverter.mapObject(sample).toJson();

		// then
		assertThat(result).contains("\"property\"");
		assertThat(result).contains("\"javaTransient\"");

		assertThat(result).doesNotContain("readOnly");
		assertThat(result).doesNotContain("annotatedTransientProperty");
	}

	@Test 
	public void writesNestedEntity() {

		Person person = new Person();
		person.birthDate = LocalDate.now();
		person.gender = Gender.MAN;
		person.address = observatoryRoad;

		Map<String, Object> sink = writeToMap(person);

		assertThat(sink.get("address")).isEqualTo(gratiotAveAsMap);
	}

	@Test 
	public void writesConcreteList() {

		Person ginger = new Person();
		ginger.setId("ginger");
		ginger.setGender(Gender.MAN);

		sarahConnor.coWorkers = Arrays.asList(kyleReese, ginger);

		Map<String, Object> target = writeToMap(sarahConnor);
		assertThat((List<Document>) target.get("coWorkers")).hasSize(2).contains(kyleAsMap);
	}

	@Test 
	public void writesInterfaceList() {

		Inventory gun = new Gun("Glock 19", 33);
		Inventory grenade = new Grenade("40 mm");

		sarahConnor.inventoryList = Arrays.asList(gun, grenade);

		Map<String, Object> target = writeToMap(sarahConnor);
		assertThat((List<Document>) target.get("inventoryList")).containsExactly(gunAsMap, grenadeAsMap);
	}

	@Test 
	public void readTypeCorrectly() {

		Person target = mappingElasticsearchConverter.read(Person.class, sarahAsMap);

		assertThat(target).isEqualTo(sarahConnor);
	}

	@Test 
	public void readListOfConcreteTypesCorrectly() {

		sarahAsMap.put("coWorkers", Collections.singletonList(kyleAsMap));

		Person target = mappingElasticsearchConverter.read(Person.class, sarahAsMap);

		assertThat(target.getCoWorkers()).contains(kyleReese);
	}

	@Test 
	public void writeMapOfConcreteType() {

		sarahConnor.shippingAddresses = new LinkedHashMap<>();
		sarahConnor.shippingAddresses.put("home", observatoryRoad);

		Map<String, Object> target = writeToMap(sarahConnor);
		assertThat(target.get("shippingAddresses")).isInstanceOf(Map.class);
		assertThat(target.get("shippingAddresses")).isEqualTo(Collections.singletonMap("home", gratiotAveAsMap));
	}

	@Test 
	public void writeMapOfInterfaceType() {

		sarahConnor.inventoryMap = new LinkedHashMap<>();
		sarahConnor.inventoryMap.put("glock19", gun);
		sarahConnor.inventoryMap.put("40 mm grenade", grenade);

		Map<String, Object> target = writeToMap(sarahConnor);
		assertThat(target.get("inventoryMap")).isInstanceOf(Map.class);
		assertThat((Map<String, Document>) target.get("inventoryMap")).containsEntry("glock19", gunAsMap)
				.containsEntry("40 mm grenade", grenadeAsMap);
	}

	@Test 
	public void readConcreteMapCorrectly() {

		sarahAsMap.put("shippingAddresses", Collections.singletonMap("home", gratiotAveAsMap));

		Person target = mappingElasticsearchConverter.read(Person.class, sarahAsMap);

		assertThat(target.getShippingAddresses()).hasSize(1).containsEntry("home", observatoryRoad);
	}

	@Test 
	public void genericWriteList() {

		Skynet skynet = new Skynet();
		skynet.objectList = new ArrayList<>();
		skynet.objectList.add(t800);
		skynet.objectList.add(gun);

		Map<String, Object> target = writeToMap(skynet);

		assertThat((List<Object>) target.get("objectList")).containsExactly(t800AsMap, gunAsMap);
	}

	@Test 
	public void genericWriteListWithList() {

		Skynet skynet = new Skynet();
		skynet.objectList = new ArrayList<>();
		skynet.objectList.add(Arrays.asList(t800, gun));

		Map<String, Object> target = writeToMap(skynet);

		assertThat((List<Object>) target.get("objectList")).containsExactly(Arrays.asList(t800AsMap, gunAsMap));
	}


	@Test 
	public void writeGenericMap() {

		Skynet skynet = new Skynet();
		skynet.objectMap = new LinkedHashMap<>();
		skynet.objectMap.put("gun", gun);
		skynet.objectMap.put("grenade", grenade);

		Map<String, Object> target = writeToMap(skynet);

		assertThat((Map<String, Object>) target.get("objectMap")).containsEntry("gun", gunAsMap).containsEntry("grenade",
				grenadeAsMap);
	}

	@Test 
	public void writeGenericMapMap() {

		Skynet skynet = new Skynet();
		skynet.objectMap = new LinkedHashMap<>();
		skynet.objectMap.put("inventory", Collections.singletonMap("glock19", gun));

		Map<String, Object> target = writeToMap(skynet);

		assertThat((Map<String, Object>) target.get("objectMap")).containsEntry("inventory",
				Collections.singletonMap("glock19", gunAsMap));
	}

	@Test 
	public void readsNestedEntity() {

		sarahAsMap.put("address", gratiotAveAsMap);

		Person target = mappingElasticsearchConverter.read(Person.class, sarahAsMap);

		assertThat(target.getAddress()).isEqualTo(observatoryRoad);
	}

	@Test 
	public void readsNestedObjectEntity() {

		Document source = Document.create();
		source.put("object", t800AsMap);

		Skynet target = mappingElasticsearchConverter.read(Skynet.class, source);

		assertThat(target.getObject()).isInstanceOf(MapDocument.class);
		MapDocument document = (MapDocument) target.getObject();
		assertThat(document.get("id")).isEqualTo(t800.id);
		assertThat(document.get("name")).isEqualTo(t800.name);
		assertThat(document.get("gender")).isEqualTo(t800.gender.name());
	}

	@Test 
	public void writesAliased() {
		assertThat(writeToMap(rifle)).doesNotContainValue(Rifle.class.getName());
	}

	@Test 
	public void writesNestedAliased() {

		t800.inventoryList = Collections.singletonList(rifle);
		Map<String, Object> target = writeToMap(t800);

		assertThat((List<Document>) target.get("inventoryList")).contains(rifleAsMap);
	}

	@Test 
	public void appliesCustomConverterForWrite() {
		assertThat(writeToMap(shotGun)).isEqualTo(shotGunAsMap);
	}

	@Test 
	public void writeSubTypeCorrectly() {

		sarahConnor.address = bigBunsCafe;

		Map<String, Object> target = writeToMap(sarahConnor);

		assertThat(target.get("address")).isEqualTo(bigBunsCafeAsMap);
	}

	@Test 
	public void readSubTypeCorrectly() {

		sarahAsMap.put("address", bigBunsCafeAsMap);

		Person target = mappingElasticsearchConverter.read(Person.class, sarahAsMap);

		assertThat(target.address).isEqualTo(bigBunsCafe);
	}

	@Test // DATAES-716
	void shouldWriteLocalDate() throws JSONException {
		Person person = new Person();
		person.setId("4711");
		person.setFirstName("John");
		person.setLastName("Doe");
		person.birthDate = LocalDate.of(2000, 8, 22);
		person.gender = Gender.MAN;

		String expected = '{' + //
				"  \"id\": \"4711\"," + //
				"  \"first-name\": \"John\"," + //
				"  \"last-name\": \"Doe\"," + //
				"  \"birth-date\": \"22.08.2000\"," + //
				"  \"gender\": \"MAN\"" + //
				'}';
		Document document = Document.create();
		mappingElasticsearchConverter.write(person, document);
		String json = document.toJson();

		assertEquals(expected, json, false);
	}

	@Test // DATAES-924
	@DisplayName("should write list of LocalDate")
	void shouldWriteListOfLocalDate() throws JSONException {

		LocalDatesEntity entity = new LocalDatesEntity();
		entity.setId("4711");
		entity.setDates(Arrays.asList(LocalDate.of(2020, 9, 15), LocalDate.of(2019, 5, 1)));
		String expected = "{\n" +
						  "  \"id\": \"4711\",\n" +
						  "  \"dates\": [\"15.09.2020\", \"01.05.2019\"]\n" +
						  "}\n"; //

		Document document = Document.create();
		mappingElasticsearchConverter.write(entity, document);
		String json = document.toJson();

		assertEquals(expected, json, false);
	}

	@Test // DATAES-716
	void shouldReadLocalDate() {
		Document document = Document.create();
		document.put("id", "4711");
		document.put("first-name", "John");
		document.put("last-name", "Doe");
		document.put("birth-date", "22.08.2000");
		document.put("gender", "MAN");

		Person person = mappingElasticsearchConverter.read(Person.class, document);

		assertThat(person.getId()).isEqualTo("4711");
		assertThat(person.getBirthDate()).isEqualTo(LocalDate.of(2000, 8, 22));
		assertThat(person.getGender()).isEqualTo(Gender.MAN);
	}

	@Test // DATAES-924
	@DisplayName("should read list of LocalDate")
	void shouldReadListOfLocalDate() {

		Document document = Document.create();
		document.put("id", "4711");
		document.put("dates", new String[] { "15.09.2020", "01.05.2019" });

		LocalDatesEntity entity = mappingElasticsearchConverter.read(LocalDatesEntity.class, document);

		assertThat(entity.getId()).isEqualTo("4711");
		assertThat(entity.getDates()).hasSize(2).containsExactly(LocalDate.of(2020, 9, 15), LocalDate.of(2019, 5, 1));
	}

	@Test // DATAES-763
	void writeEntityWithMapDataType() {

		Notification notification = new Notification();
		notification.setFromEmail("from@email.com");
		notification.setToEmail("to@email.com");
		Map<String, Object> data = new HashMap<>();
		data.put("documentType", "abc");
		data.put("content", null);
		notification.params = data;
		notification.id = 1L;

		Document document = Document.create();
		mappingElasticsearchConverter.write(notification, document);
		assertThat(document).isEqualTo(notificationAsMap);
	}

	@Test // DATAES-763
	void readEntityWithMapDataType() {

		Document document = Document.create();
		document.put("id", 1L);
		document.put("fromEmail", "from@email.com");
		document.put("toEmail", "to@email.com");
		Map<String, Object> data = new HashMap<>();
		data.put("documentType", "abc");
		data.put("content", null);
		document.put("params", data);

		Notification notification = mappingElasticsearchConverter.read(Notification.class, document);
		assertThat(notification.params.get("documentType")).isEqualTo("abc");
		assertThat(notification.params.get("content")).isNull();
	}

	@Test // DATAES-795
	void readGenericMapWithSimpleTypes() {
		Map<String, Object> mapWithSimpleValues = new HashMap<>();
		mapWithSimpleValues.put("int", 1);
		mapWithSimpleValues.put("string", "string");
		mapWithSimpleValues.put("boolean", true);

		Document document = Document.create();
		document.put("schemaLessObject", mapWithSimpleValues);

		SchemaLessObjectWrapper wrapper = mappingElasticsearchConverter.read(SchemaLessObjectWrapper.class, document);
		assertThat(wrapper.getSchemaLessObject()).isEqualTo(mapWithSimpleValues);
	}

	@Test // DATAES-797
	void readGenericListWithMaps() {
		Map<String, Object> simpleMap = new HashMap<>();
		simpleMap.put("int", 1);

		List<Map<String, Object>> listWithSimpleMap = new ArrayList<>();
		listWithSimpleMap.add(simpleMap);

		Map<String, List<Map<String, Object>>> mapWithSimpleList = new HashMap<>();
		mapWithSimpleList.put("someKey", listWithSimpleMap);

		Document document = Document.create();
		document.put("schemaLessObject", mapWithSimpleList);

		SchemaLessObjectWrapper wrapper = mappingElasticsearchConverter.read(SchemaLessObjectWrapper.class, document);
		assertThat(wrapper.getSchemaLessObject()).isEqualTo(mapWithSimpleList);
	}

	@Test // DATAES-799
	void shouldNotWriteSeqNoPrimaryTermProperty() {
		EntityWithSeqNoPrimaryTerm entity = new EntityWithSeqNoPrimaryTerm();
		entity.seqNoPrimaryTerm = new SeqNoPrimaryTerm(1L, 2L);
		Document document = Document.create();

		mappingElasticsearchConverter.write(entity, document);

		assertThat(document).doesNotContainKey("seqNoPrimaryTerm");
	}

	@Test // DATAES-799
	void shouldNotReadSeqNoPrimaryTermProperty() {
		Document document = Document.create().append("seqNoPrimaryTerm", emptyMap());

		EntityWithSeqNoPrimaryTerm entity = mappingElasticsearchConverter.read(EntityWithSeqNoPrimaryTerm.class, document);

		assertThat(entity.seqNoPrimaryTerm).isNull();
	}

	@Test // DATAES-845
	void shouldWriteCollectionsWithNullValues() throws JSONException {
		EntityWithListProperty entity = new EntityWithListProperty();
		entity.setId("42");
		entity.setValues(Arrays.asList(null, "two", null, "four"));

		String expected = '{' + //
				"  \"id\": \"42\"," + //
				"  \"values\": [null, \"two\", null, \"four\"]" + //
				'}';
		Document document = Document.create();
		mappingElasticsearchConverter.write(entity, document);
		String json = document.toJson();

		assertEquals(expected, json, false);
	}

	@Test // DATAES-865
	void shouldWriteEntityWithMapAsObject() throws JSONException {

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("foo", "bar");

		EntityWithObject entity = new EntityWithObject();
		entity.setId("42");
		entity.setContent(map);

		String expected = "{\n" +
						  "  \"id\": \"42\",\n" +
						  "  \"content\": {\n" +
						  "    \"foo\": \"bar\"\n" +
						  "  }\n" +
						  "}\n"; //

		Document document = Document.create();

		mappingElasticsearchConverter.write(entity, document);

		assertEquals(expected, document.toJson(), false);
	}

	@Test // DATAES-920
	@DisplayName("should write null value if configured")
	void shouldWriteNullValueIfConfigured() throws JSONException {

		EntityWithNullField entity = new EntityWithNullField();
		entity.setId("42");

		String expected = "{\n" +
						  "  \"id\": \"42\",\n" +
						  "  \"saved\": null\n" +
						  "}\n"; //

		Document document = Document.create();

		mappingElasticsearchConverter.write(entity, document);

		assertEquals(expected, document.toJson(), false);
	}

	@Nested
	class RangeTests {

		static final String JSON = "{"
				+ "\"integerRange\":{\"gt\":\"1\",\"lt\":\"10\"}," //
				+ "\"floatRange\":{\"gte\":\"1.2\",\"lte\":\"2.5\"}," //
				+ "\"longRange\":{\"gt\":\"2\",\"lte\":\"5\"}," //
				+ "\"doubleRange\":{\"gte\":\"3.2\",\"lt\":\"7.4\"}," //
				+ "\"dateRange\":{\"gte\":\"1970-01-01T00:00:00.000Z\",\"lte\":\"1970-01-01T01:00:00.000Z\"}," //
				+ "\"localDateRange\":{\"gte\":\"2021-07-06\"}," //
				+ "\"localTimeRange\":{\"gte\":\"00:30:00.000\",\"lt\":\"02:30:00.000\"}," //
				+ "\"localDateTimeRange\":{\"gt\":\"2021-01-01T00:30:00.000\",\"lt\":\"2021-01-01T02:30:00.000\"}," //
				+ "\"offsetTimeRange\":{\"gte\":\"00:30:00.000+02:00\",\"lt\":\"02:30:00.000+02:00\"}," //
				+ "\"zonedDateTimeRange\":{\"gte\":\"2021-01-01T00:30:00.000+02:00\",\"lte\":\"2021-01-01T00:30:00.000+02:00\"}," //
				+ "\"nullRange\":null}";

		@Test
		public void shouldReadRanges() throws JSONException {

			// given
			Document source = Document.parse(JSON);

			// when
			RangeEntity entity = mappingElasticsearchConverter.read(RangeEntity.class, source);

			// then
			assertThat(entity) //
					.isNotNull() //
					.satisfies(e -> {
						assertThat(e.getIntegerRange()).isEqualTo(Range.open(1, 10));
						assertThat(e.getFloatRange()).isEqualTo(Range.closed(1.2f, 2.5f));
						assertThat(e.getLongRange()).isEqualTo(Range.leftOpen(2l, 5l));
						assertThat(e.getDoubleRange()).isEqualTo(Range.rightOpen(3.2d, 7.4d));
						assertThat(e.getDateRange()).isEqualTo(Range.closed(new Date(0), new Date(60 * 60 * 1000)));
						assertThat(e.getLocalDateRange())
								.isEqualTo(Range.rightUnbounded(Range.Bound.inclusive(LocalDate.of(2021, 7, 6))));
						assertThat(e.getLocalTimeRange()).isEqualTo(Range.rightOpen(LocalTime.of(0, 30), LocalTime.of(2, 30)));
						assertThat(e.getLocalDateTimeRange())
								.isEqualTo(Range.open(LocalDateTime.of(2021, 1, 1, 0, 30), LocalDateTime.of(2021, 1, 1, 2, 30)));
						assertThat(e.getOffsetTimeRange())
								.isEqualTo(Range.rightOpen(OffsetTime.of(LocalTime.of(0, 30), ZoneOffset.ofHours(2)),
										OffsetTime.of(LocalTime.of(2, 30), ZoneOffset.ofHours(2))));
						assertThat(e.getZonedDateTimeRange()).isEqualTo(
								Range.just(ZonedDateTime.of(LocalDate.of(2021, 1, 1), LocalTime.of(0, 30), ZoneOffset.ofHours(2))));
						assertThat(e.getNullRange()).isNull();
					});
		}

		@Test
		public void shouldWriteRanges() throws JSONException {

			// given
			Document source = Document.parse(JSON);
			RangeEntity entity = new RangeEntity();
			entity.setIntegerRange(Range.open(1, 10));
			entity.setFloatRange(Range.closed(1.2f, 2.5f));
			entity.setLongRange(Range.leftOpen(2l, 5l));
			entity.setDoubleRange(Range.rightOpen(3.2d, 7.4d));
			entity.setDateRange(Range.closed(new Date(0), new Date(60 * 60 * 1000)));
			entity.setLocalDateRange(Range.rightUnbounded(Range.Bound.inclusive(LocalDate.of(2021, 7, 6))));
			entity.setLocalTimeRange(Range.rightOpen(LocalTime.of(0, 30), LocalTime.of(2, 30)));
			entity
					.setLocalDateTimeRange(Range.open(LocalDateTime.of(2021, 1, 1, 0, 30), LocalDateTime.of(2021, 1, 1, 2, 30)));
			entity.setOffsetTimeRange(Range.rightOpen(OffsetTime.of(LocalTime.of(0, 30), ZoneOffset.ofHours(2)),
					OffsetTime.of(LocalTime.of(2, 30), ZoneOffset.ofHours(2))));
			entity.setZonedDateTimeRange(
					Range.just(ZonedDateTime.of(LocalDate.of(2021, 1, 1), LocalTime.of(0, 30), ZoneOffset.ofHours(2))));
			entity.setNullRange(null);

			// when
			Document document = mappingElasticsearchConverter.mapObject(entity);

			// then
			assertThat(document).isEqualTo(source);
		}

		@com.mawen.search.core.annotation.Document(indexName = "test-index-range-entity-mapper")
		class RangeEntity {

			@Id private String id;
			@Field(type = FieldType.Integer_Range) private Range<Integer> integerRange;
			@Field(type = FieldType.Float_Range) private Range<Float> floatRange;
			@Field(type = FieldType.Long_Range) private Range<Long> longRange;
			@Field(type = FieldType.Double_Range) private Range<Double> doubleRange;
			@Field(type = FieldType.Date_Range) private Range<Date> dateRange;
			@Field(type = FieldType.Date_Range, format = DateFormat.year_month_day) private Range<LocalDate> localDateRange;
			@Field(type = FieldType.Date_Range,
					format = DateFormat.hour_minute_second_millis) private Range<LocalTime> localTimeRange;
			@Field(type = FieldType.Date_Range,
					format = DateFormat.date_hour_minute_second_millis) private Range<LocalDateTime> localDateTimeRange;
			@Field(type = FieldType.Date_Range, format = DateFormat.time) private Range<OffsetTime> offsetTimeRange;
			@Field(type = FieldType.Date_Range) private Range<ZonedDateTime> zonedDateTimeRange;
			@Field(type = FieldType.Date_Range, storeNullValue = true) private Range<ZonedDateTime> nullRange;

			public String getId() {
				return id;
			}

			public Range<Integer> getIntegerRange() {
				return integerRange;
			}

			public Range<Float> getFloatRange() {
				return floatRange;
			}

			public Range<Long> getLongRange() {
				return longRange;
			}

			public Range<Double> getDoubleRange() {
				return doubleRange;
			}

			public Range<Date> getDateRange() {
				return dateRange;
			}

			public Range<LocalDate> getLocalDateRange() {
				return localDateRange;
			}

			public Range<LocalTime> getLocalTimeRange() {
				return localTimeRange;
			}

			public Range<LocalDateTime> getLocalDateTimeRange() {
				return localDateTimeRange;
			}

			public Range<OffsetTime> getOffsetTimeRange() {
				return offsetTimeRange;
			}

			public Range<ZonedDateTime> getZonedDateTimeRange() {
				return zonedDateTimeRange;
			}

			public Range<ZonedDateTime> getNullRange() {
				return nullRange;
			}

			public void setId(String id) {
				this.id = id;
			}

			public void setIntegerRange(Range<Integer> integerRange) {
				this.integerRange = integerRange;
			}

			public void setFloatRange(Range<Float> floatRange) {
				this.floatRange = floatRange;
			}

			public void setLongRange(Range<Long> longRange) {
				this.longRange = longRange;
			}

			public void setDoubleRange(Range<Double> doubleRange) {
				this.doubleRange = doubleRange;
			}

			public void setDateRange(Range<Date> dateRange) {
				this.dateRange = dateRange;
			}

			public void setLocalDateRange(Range<LocalDate> localDateRange) {
				this.localDateRange = localDateRange;
			}

			public void setLocalTimeRange(Range<LocalTime> localTimeRange) {
				this.localTimeRange = localTimeRange;
			}

			public void setLocalDateTimeRange(Range<LocalDateTime> localDateTimeRange) {
				this.localDateTimeRange = localDateTimeRange;
			}

			public void setOffsetTimeRange(Range<OffsetTime> offsetTimeRange) {
				this.offsetTimeRange = offsetTimeRange;
			}

			public void setZonedDateTimeRange(Range<ZonedDateTime> zonedDateTimeRange) {
				this.zonedDateTimeRange = zonedDateTimeRange;
			}

			public void setNullRange(Range<ZonedDateTime> nullRange) {
				this.nullRange = nullRange;
			}

		}
	}

	@Test // #1945
	@DisplayName("should write using ValueConverters")
	void shouldWriteUsingValueConverters() throws JSONException {

		EntityWithCustomValueConverters entity = new EntityWithCustomValueConverters();
		entity.setId("42");
		entity.setFieldWithClassBasedConverter("classbased");
		entity.setFieldWithEnumBasedConverter("enumbased");
		entity.setDontConvert("Monty Python's Flying Circus");

		String expected = "{\n" +
						  "  \"id\": \"42\",\n" +
						  "  \"fieldWithClassBasedConverter\": \"desabssalc\",\n" +
						  "  \"fieldWithEnumBasedConverter\": \"desabmune\",\n" +
						  "  \"dontConvert\": \"Monty Python's Flying Circus\"\n" +
						  "}\n"; //

		Document document = Document.create();

		mappingElasticsearchConverter.write(entity, document);

		assertEquals(expected, document.toJson(), false);
	}

	@Test // #1945
	@DisplayName("should read using ValueConverters")
	void shouldReadUsingValueConverters() throws JSONException {

		String json = "{\n" +
					  "  \"id\": \"42\",\n" +
					  "  \"fieldWithClassBasedConverter\": \"desabssalc\",\n" +
					  "  \"fieldWithEnumBasedConverter\": \"desabmune\",\n" +
					  "  \"dontConvert\": \"Monty Python's Flying Circus\"\n" +
					  "}\n"; //

		Document source = Document.parse(json);

		// when
		EntityWithCustomValueConverters entity = mappingElasticsearchConverter.read(EntityWithCustomValueConverters.class,
				source);

		assertThat(entity.getId()).isEqualTo("42");
		assertThat(entity.getFieldWithClassBasedConverter()).isEqualTo("classbased");
		assertThat(entity.getFieldWithEnumBasedConverter()).isEqualTo("enumbased");
		assertThat(entity.getDontConvert()).isEqualTo("Monty Python's Flying Circus");
	}

	@Test // #2080
	@DisplayName("should not try to call property converter on updating criteria exists")
	void shouldNotTryToCallPropertyConverterOnUpdatingCriteriaExists() {

		// don't care if the query makes no sense, we just add all criteria without values
		Query query = new CriteriaQuery(Criteria.where("fieldWithClassBasedConverter").exists().empty().notEmpty());

		mappingElasticsearchConverter.updateQuery(query, EntityWithCustomValueConverters.class);
	}

	@Test // #2280
	@DisplayName("should read a single String into a List property")
	void shouldReadASingleStringIntoAListProperty() {

		@Language("JSON")
		String json = "{\n" +
				   "	\"stringList\": \"foo\"\n" +
				   "}\n";
		Document source = Document.parse(json);

		EntityWithCollections entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getStringList()).containsExactly("foo");
	}

	@Test // #2280
	@DisplayName("should read a String array into a List property")
	void shouldReadAStringArrayIntoAListProperty() {

		@Language("JSON")
		String json = "{\n" +
				   "	\"stringList\": [\"foo\", \"bar\"]\n" +
				   "}\n";
		Document source = Document.parse(json);

		EntityWithCollections entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getStringList()).containsExactly("foo", "bar");
	}

	@Test // #2280
	@DisplayName("should read a single String into a Set property")
	void shouldReadASingleStringIntoASetProperty() {

		@Language("JSON")
		String json = "{\n" +
				   "	\"stringSet\": \"foo\"\n" +
				   "}\n";
		Document source = Document.parse(json);

		EntityWithCollections entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getStringSet()).containsExactly("foo");
	}

	@Test // #2280
	@DisplayName("should read a String array into a Set property")
	void shouldReadAStringArrayIntoASetProperty() {

		@Language("JSON")
		String json = "{\n" +
				   "	\"stringSet\": [\"foo\", \"bar\"]\n" +
				   "}\n";
		Document source = Document.parse(json);

		EntityWithCollections entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getStringSet()).containsExactly("foo", "bar");
	}

	@Test // #2280
	@DisplayName("should read a single object into a List property")
	void shouldReadASingleObjectIntoAListProperty() {

		@Language("JSON")
		String json = "{\n" +
				   "	\"childrenList\": {\n" +
				   "		\"name\": \"child\"\n" +
				   "	}\n" +
				   "}\n";
		Document source = Document.parse(json);

		EntityWithCollections entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getChildrenList()).hasSize(1);
		// noinspection ConstantConditions
		assertThat(entity.getChildrenList().get(0).getName()).isEqualTo("child");
	}

	@Test // #2280
	@DisplayName("should read an object array into a List property")
	void shouldReadAnObjectArrayIntoAListProperty() {

		@Language("JSON")
		String json = "{\n" +
				   "	\"childrenList\": [\n" +
				   "		{\n" +
				   "			\"name\": \"child1\"\n" +
				   "		},\n" +
				   "		{\n" +
				   "			\"name\": \"child2\"\n" +
				   "		}\n" +
				   "	]\n" +
				   "}\n";
		Document source = Document.parse(json);

		EntityWithCollections entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getChildrenList()).hasSize(2);
		// noinspection ConstantConditions
		assertThat(entity.getChildrenList().get(0).getName()).isEqualTo("child1");
		assertThat(entity.getChildrenList().get(1).getName()).isEqualTo("child2");
	}

	@Test // #2280
	@DisplayName("should read a single object into a Set property")
	void shouldReadASingleObjectIntoASetProperty() {

		@Language("JSON")
		String json = "{\n" +
				   "	\"childrenSet\": {\n" +
				   "		\"name\": \"child\"\n" +
				   "	}\n" +
				   "}\n";
		Document source = Document.parse(json);

		EntityWithCollections entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getChildrenSet()).hasSize(1);
		// noinspection ConstantConditions
		assertThat(entity.getChildrenSet().iterator().next().getName()).isEqualTo("child");
	}

	@Test // #2280
	@DisplayName("should read an object array into a Set property")
	void shouldReadAnObjectArrayIntoASetProperty() {

		@Language("JSON")
		String json = "{\n" +
					  "	\"childrenSet\": [\n" +
					  "		{\n" +
					  "			\"name\": \"child1\"\n" +
					  "		},\n" +
					  "		{\n" +
					  "			\"name\": \"child2\"\n" +
					  "		}\n" +
					  "	]\n" +
					  "}\n";
		Document source = Document.parse(json);

		EntityWithCollections entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getChildrenSet()).hasSize(2);
		// noinspection ConstantConditions
		List<String> names = entity.getChildrenSet().stream().map(EntityWithCollections.Child::getName)
				.collect(Collectors.toList());
		assertThat(names).containsExactlyInAnyOrder("child1", "child2");
	}

	@Test // #2280
	@DisplayName("should read a single String into a List property immutable")
	void shouldReadASingleStringIntoAListPropertyImmutable() {

		@Language("JSON")
		String json = "{\n" +
				   "	\"stringList\": \"foo\"\n" +
				   "}\n";
		Document source = Document.parse(json);

		ImmutableEntityWithCollections entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getStringList()).containsExactly("foo");
	}

	@Test // #2280
	@DisplayName("should read a String array into a List property immutable")
	void shouldReadAStringArrayIntoAListPropertyImmutable() {

		@Language("JSON")
		String json = "{\n" +
					  "	\"stringList\": [\"foo\", \"bar\"]\n" +
					  "}\n";
		Document source = Document.parse(json);

		ImmutableEntityWithCollections entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getStringList()).containsExactly("foo", "bar");
	}

	@Test // #2280
	@DisplayName("should read a single String into a Set property immutable")
	void shouldReadASingleStringIntoASetPropertyImmutable() {

		@Language("JSON")
		String json = "{\n" +
					  "	\"stringSet\": \"foo\"\n" +
					  "}\n";
		Document source = Document.parse(json);

		ImmutableEntityWithCollections entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getStringSet()).containsExactly("foo");
	}

	@Test // #2280
	@DisplayName("should read a String array into a Set property immutable")
	void shouldReadAStringArrayIntoASetPropertyImmutable() {

		@Language("JSON")
		String json = "{\n" +
				   "	\"stringSet\": [\"foo\", \"bar\"]\n" +
				   "}\n";
		Document source = Document.parse(json);

		ImmutableEntityWithCollections entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getStringSet()).containsExactly("foo", "bar");
	}

	@Test // #2280
	@DisplayName("should read a single object into a List property immutable")
	void shouldReadASingleObjectIntoAListPropertyImmutable() {

		@Language("JSON")
		String json = "{\n" +
					  "	\"childrenList\": {\n" +
					  "		\"name\": \"child\"\n" +
					  "	}\n" +
					  "}\n";
		Document source = Document.parse(json);

		ImmutableEntityWithCollections entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getChildrenList()).hasSize(1);
		// noinspection ConstantConditions
		assertThat(entity.getChildrenList().get(0).getName()).isEqualTo("child");
	}

	@Test // #2280
	@DisplayName("should read an object array into a List property immutable")
	void shouldReadAnObjectArrayIntoAListPropertyImmutable() {

		@Language("JSON")
		String json = "{\n" +
				   "	\"childrenList\": [\n" +
				   "		{\n" +
				   "			\"name\": \"child1\"\n" +
				   "		},\n" +
				   "		{\n" +
				   "			\"name\": \"child2\"\n" +
				   "		}\n" +
				   "	]\n" +
				   "}\n";
		Document source = Document.parse(json);

		ImmutableEntityWithCollections entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getChildrenList()).hasSize(2);
		// noinspection ConstantConditions
		assertThat(entity.getChildrenList().get(0).getName()).isEqualTo("child1");
		assertThat(entity.getChildrenList().get(1).getName()).isEqualTo("child2");
	}

	@Test // #2280
	@DisplayName("should read a single object into a Set property immutable")
	void shouldReadASingleObjectIntoASetPropertyImmutable() {

		@Language("JSON")
		String json = "{\n" +
				   "	\"childrenSet\": {\n" +
				   "		\"name\": \"child\"\n" +
				   "	}\n" +
				   "}\n";
		Document source = Document.parse(json);

		ImmutableEntityWithCollections entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getChildrenSet()).hasSize(1);
		// noinspection ConstantConditions
		assertThat(entity.getChildrenSet().iterator().next().getName()).isEqualTo("child");
	}

	@Test // #2280
	@DisplayName("should read an object array into a Set property immutable")
	void shouldReadAnObjectArrayIntoASetPropertyImmutable() {

		@Language("JSON")
		String json = "{\n" +
					  "	\"childrenSet\": [\n" +
					  "		{\n" +
					  "			\"name\": \"child1\"\n" +
					  "		},\n" +
					  "		{\n" +
					  "			\"name\": \"child2\"\n" +
					  "		}\n" +
					  "	]\n" +
					  "}\n";
		Document source = Document.parse(json);

		ImmutableEntityWithCollections entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getChildrenSet()).hasSize(2);
		// noinspection ConstantConditions
		List<String> names = entity.getChildrenSet().stream().map(ImmutableEntityWithCollections.Child::getName)
				.collect(Collectors.toList());
		assertThat(names).containsExactlyInAnyOrder("child1", "child2");
	}

	private Map<String, Object> writeToMap(Object source) {

		Document sink = Document.create();
		mappingElasticsearchConverter.write(source, sink);
		return sink;
	}

	@Test // #2290
	@DisplayName("should respect field setting for empty properties")
	void shouldRespectFieldSettingForEmptyProperties() throws JSONException {
		@Language("JSON")
		String expected = "{\n" +
					   "	\"id\": \"42\",\n" +
					   "	\"stringToWriteWhenEmpty\": \"\",\n" +
					   "	\"listToWriteWhenEmpty\": [],\n" +
					   "	\"mapToWriteWhenEmpty\": {}\n" +
					   "}\n";
		EntityWithPropertiesThatMightBeEmpty entity = new EntityWithPropertiesThatMightBeEmpty();
		entity.setId("42");
		entity.setStringToWriteWhenEmpty("");
		entity.setStringToNotWriteWhenEmpty("");
		entity.setListToWriteWhenEmpty(emptyList());
		entity.setListToNotWriteWhenEmpty(emptyList());
		entity.setMapToWriteWhenEmpty(emptyMap());
		entity.setMapToNotWriteWhenEmpty(emptyMap());

		Document document = Document.create();
		mappingElasticsearchConverter.write(entity, document);
		String json = document.toJson();

		assertEquals(expected, json, true);
	}

	@Test // #2502
	@DisplayName("should write entity with dotted field name")
	void shouldWriteEntityWithDottedFieldName() throws JSONException {

		@Language("JSON")
		String expected = "	{\n" +
					   "		\"id\": \"42\",\n" +
					   "		\"dotted.field\": \"dotted field\"\n" +
					   "	}\n";
		FieldNameDotsEntity entity = new FieldNameDotsEntity();
		entity.setId("42");
		entity.setDottedField("dotted field");

		Document document = Document.create();
		mappingElasticsearchConverter.write(entity, document);
		String json = document.toJson();

		assertEquals(expected, json, true);
	}

	@Test // #2502
	@DisplayName("should read entity with dotted field name")
	void shouldReadEntityWithDottedFieldName() {

		@Language("JSON")
		String json = "{\n" +
					  "  \"id\": \"42\",\n" +
					  "  \"dotted.field\": \"dotted field\"\n" +
					  "}";

		Document document = Document.parse(json);

		FieldNameDotsEntity entity = mappingElasticsearchConverter.read(FieldNameDotsEntity.class, document);

		assertThat(entity.id).isEqualTo("42");
		assertThat(entity.getDottedField()).isEqualTo("dotted field");
	}

	// region entities
	public static class Sample {
		@Nullable public @ReadOnlyProperty String readOnly;
		@Nullable public @Transient String annotatedTransientProperty;
		@Nullable public transient String javaTransientProperty;
		@Nullable public String property;

		@Nullable
		public String getReadOnly() {
			return readOnly;
		}

		public void setReadOnly(@Nullable String readOnly) {
			this.readOnly = readOnly;
		}

		@Nullable
		public String getAnnotatedTransientProperty() {
			return annotatedTransientProperty;
		}

		public void setAnnotatedTransientProperty(@Nullable String annotatedTransientProperty) {
			this.annotatedTransientProperty = annotatedTransientProperty;
		}

		@Nullable
		public String getJavaTransientProperty() {
			return javaTransientProperty;
		}

		public void setJavaTransientProperty(@Nullable String javaTransientProperty) {
			this.javaTransientProperty = javaTransientProperty;
		}

		@Nullable
		public String getProperty() {
			return property;
		}

		public void setProperty(@Nullable String property) {
			this.property = property;
		}
	}

	static class Person {
		@Nullable
		@Id String id;
		@Nullable String name;
		@Nullable
		@Field(value = "first-name") String firstName;
		@Nullable
		@Field(value = "last-name") String lastName;
		@Nullable
		@Field(value = "birth-date", type = FieldType.Date, format = {}, pattern = "dd.MM.uuuu") LocalDate birthDate;
		@Nullable Gender gender;
		@Nullable Address address;
		@Nullable List<Person> coWorkers;
		@Nullable List<Inventory> inventoryList;
		@Nullable Map<String, Address> shippingAddresses;
		@Nullable Map<String, Inventory> inventoryMap;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(@Nullable String firstName) {
			this.firstName = firstName;
		}

		@Nullable
		public String getLastName() {
			return lastName;
		}

		public void setLastName(@Nullable String lastName) {
			this.lastName = lastName;
		}

		@Nullable
		public LocalDate getBirthDate() {
			return birthDate;
		}

		public void setBirthDate(@Nullable LocalDate birthDate) {
			this.birthDate = birthDate;
		}

		@Nullable
		public Gender getGender() {
			return gender;
		}

		public void setGender(@Nullable Gender gender) {
			this.gender = gender;
		}

		@Nullable
		public Address getAddress() {
			return address;
		}

		public void setAddress(@Nullable Address address) {
			this.address = address;
		}

		@Nullable
		public List<Person> getCoWorkers() {
			return coWorkers;
		}

		public void setCoWorkers(@Nullable List<Person> coWorkers) {
			this.coWorkers = coWorkers;
		}

		@Nullable
		public List<Inventory> getInventoryList() {
			return inventoryList;
		}

		public void setInventoryList(@Nullable List<Inventory> inventoryList) {
			this.inventoryList = inventoryList;
		}

		@Nullable
		public Map<String, Address> getShippingAddresses() {
			return shippingAddresses;
		}

		public void setShippingAddresses(@Nullable Map<String, Address> shippingAddresses) {
			this.shippingAddresses = shippingAddresses;
		}

		@Nullable
		public Map<String, Inventory> getInventoryMap() {
			return inventoryMap;
		}

		public void setInventoryMap(@Nullable Map<String, Inventory> inventoryMap) {
			this.inventoryMap = inventoryMap;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Person person = (Person) o;

			if (!Objects.equals(id, person.id))
				return false;
			if (!Objects.equals(name, person.name))
				return false;
			if (!Objects.equals(firstName, person.firstName))
				return false;
			if (!Objects.equals(lastName, person.lastName))
				return false;
			if (!Objects.equals(birthDate, person.birthDate))
				return false;
			if (gender != person.gender)
				return false;
			if (!Objects.equals(address, person.address))
				return false;
			if (!Objects.equals(coWorkers, person.coWorkers))
				return false;
			if (!Objects.equals(inventoryList, person.inventoryList))
				return false;
			if (!Objects.equals(shippingAddresses, person.shippingAddresses))
				return false;
			return Objects.equals(inventoryMap, person.inventoryMap);
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (name != null ? name.hashCode() : 0);
			result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
			result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
			result = 31 * result + (birthDate != null ? birthDate.hashCode() : 0);
			result = 31 * result + (gender != null ? gender.hashCode() : 0);
			result = 31 * result + (address != null ? address.hashCode() : 0);
			result = 31 * result + (coWorkers != null ? coWorkers.hashCode() : 0);
			result = 31 * result + (inventoryList != null ? inventoryList.hashCode() : 0);
			result = 31 * result + (shippingAddresses != null ? shippingAddresses.hashCode() : 0);
			result = 31 * result + (inventoryMap != null ? inventoryMap.hashCode() : 0);
			return result;
		}
	}

	static class LocalDatesEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(value = "dates", type = FieldType.Date, format = {}, pattern = "dd.MM.uuuu") private List<LocalDate> dates;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public List<LocalDate> getDates() {
			return dates;
		}

		public void setDates(@Nullable List<LocalDate> dates) {
			this.dates = dates;
		}
	}

	enum Gender {

		MAN("1"), MACHINE("0");

		String theValue;

		Gender(String theValue) {
			this.theValue = theValue;
		}

		public String getTheValue() {
			return theValue;
		}
	}

	interface Inventory {

		String getLabel();
	}

	static class Gun implements Inventory {
		final String label;
		final int shotsPerMagazine;

		public Gun(@Nullable String label, int shotsPerMagazine) {
			this.label = label;
			this.shotsPerMagazine = shotsPerMagazine;
		}

		@Override
		public String getLabel() {
			return label;
		}

		public int getShotsPerMagazine() {
			return shotsPerMagazine;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Gun gun = (Gun) o;

			if (shotsPerMagazine != gun.shotsPerMagazine)
				return false;
			return label.equals(gun.label);
		}

		@Override
		public int hashCode() {
			int result = label.hashCode();
			result = 31 * result + shotsPerMagazine;
			return result;
		}
	}

	static class Grenade implements Inventory {
		final String label;

		public Grenade(String label) {
			this.label = label;
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Grenade))
				return false;

			return label.equals(((Grenade) o).label);
		}

		@Override
		public int hashCode() {
			return label.hashCode();
		}
	}

	@TypeAlias("rifle")
	static class Rifle implements Inventory {

		final String label;
		final double weight;
		final int maxShotsPerMagazine;

		public Rifle(String label, double weight, int maxShotsPerMagazine) {
			this.label = label;
			this.weight = weight;
			this.maxShotsPerMagazine = maxShotsPerMagazine;
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Rifle))
				return false;

			Rifle rifle = (Rifle) o;
			if (Double.compare(rifle.weight, weight) != 0)
				return false;
			if (maxShotsPerMagazine != rifle.maxShotsPerMagazine)
				return false;
			return label.equals(rifle.label);
		}

		@Override
		public int hashCode() {
			int result;
			long temp;
			result = label.hashCode();
			temp = Double.doubleToLongBits(weight);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			result = 31 * result + maxShotsPerMagazine;
			return result;
		}
	}

	static class ShotGun implements Inventory {

		private final String label;

		public ShotGun(String label) {
			this.label = label;
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof ShotGun ))
				return false;

			return label.equals(((ShotGun) o).label);
		}

		@Override
		public int hashCode() {
			return label.hashCode();
		}
	}

	static class Address {
		@Nullable private Point location;
		@Nullable private String street;
		@Nullable private String city;

		@Nullable
		public Point getLocation() {
			return location;
		}

		public void setLocation(@Nullable Point location) {
			this.location = location;
		}

		@Nullable
		public String getStreet() {
			return street;
		}

		public void setStreet(@Nullable String street) {
			this.street = street;
		}

		@Nullable
		public String getCity() {
			return city;
		}

		public void setCity(@Nullable String city) {
			this.city = city;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Address))
				return false;

			Address address = (Address) o;
			if (!Objects.equals(location, address.location))
				return false;
			if (!Objects.equals(street, address.street))
				return false;
			return Objects.equals(city, address.city);
		}

		@Override
		public int hashCode() {
			int result = location != null ? location.hashCode() : 0;
			result = 31 * result + (street != null ? street.hashCode() : 0);
			result = 31 * result + (city != null ? city.hashCode() : 0);
			return result;
		}
	}

	static class Place extends Address {
		@Nullable private String name;

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Place))
				return false;

			return Objects.equals(name, ((Place) o).name);
		}

		@Override
		public int hashCode() {
			return name != null ? name.hashCode() : 0;
		}
	}

	static class Skynet {
		@Nullable private Object object;
		@Nullable private List<Object> objectList;
		@Nullable private Map<String, Object> objectMap;

		@Nullable
		public Object getObject() {
			return object;
		}

		public void setObject(@Nullable Object object) {
			this.object = object;
		}

		@Nullable
		public List<Object> getObjectList() {
			return objectList;
		}

		public void setObjectList(@Nullable List<Object> objectList) {
			this.objectList = objectList;
		}

		@Nullable
		public Map<String, Object> getObjectMap() {
			return objectMap;
		}

		public void setObjectMap(@Nullable Map<String, Object> objectMap) {
			this.objectMap = objectMap;
		}
	}

	static class Notification {
		@Nullable private Long id;
		@Nullable private String fromEmail;
		@Nullable private String toEmail;
		@Nullable private Map<String, Object> params;

		@Nullable
		public Long getId() {
			return id;
		}

		public void setId(@Nullable Long id) {
			this.id = id;
		}

		@Nullable
		public String getFromEmail() {
			return fromEmail;
		}

		public void setFromEmail(@Nullable String fromEmail) {
			this.fromEmail = fromEmail;
		}

		@Nullable
		public String getToEmail() {
			return toEmail;
		}

		public void setToEmail(@Nullable String toEmail) {
			this.toEmail = toEmail;
		}

		@Nullable
		public Map<String, Object> getParams() {
			return params;
		}

		public void setParams(@Nullable Map<String, Object> params) {
			this.params = params;
		}
	}

	@WritingConverter
	static class ShotGunToMapConverter implements Converter<ShotGun, Map<String, Object>> {

		@Override
		public Map<String, Object> convert(ShotGun source) {

			LinkedHashMap<String, Object> target = new LinkedHashMap<>();
			target.put("model", source.getLabel());
			return target;
		}
	}

	@ReadingConverter
	static class MapToShotGunConverter implements Converter<Map<String, Object>, ShotGun> {

		@Override
		public ShotGun convert(Map<String, Object> source) {
			return new ShotGun(source.get("model").toString());
		}
	}

	static class Car {
		@Nullable private String name;
		@Nullable private String model;

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getModel() {
			return model;
		}

		public void setModel(@Nullable String model) {
			this.model = model;
		}
	}

	@com.mawen.search.core.annotation.Document(indexName = "test-index-geo-core-entity-mapper")
	static class GeoEntity {
		@Nullable
		@Id private String id;
		// geo shape - Spring Data
		@Nullable private Box box;
		@Nullable private Circle circle;
		@Nullable private Polygon polygon;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public Box getBox() {
			return box;
		}

		public void setBox(@Nullable Box box) {
			this.box = box;
		}

		@Nullable
		public Circle getCircle() {
			return circle;
		}

		public void setCircle(@Nullable Circle circle) {
			this.circle = circle;
		}

		@Nullable
		public Polygon getPolygon() {
			return polygon;
		}

		public void setPolygon(@Nullable Polygon polygon) {
			this.polygon = polygon;
		}
	}

	static class SchemaLessObjectWrapper {
		@Nullable private Map<String, Object> schemaLessObject;

		@Nullable
		public Map<String, Object> getSchemaLessObject() {
			return schemaLessObject;
		}

		public void setSchemaLessObject(@Nullable Map<String, Object> schemaLessObject) {
			this.schemaLessObject = schemaLessObject;
		}
	}

	@com.mawen.search.core.annotation.Document(
			indexName = "test-index-entity-with-seq-no-primary-term-mapper")
	static class EntityWithSeqNoPrimaryTerm {
		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm;

		@Nullable
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return seqNoPrimaryTerm;
		}

		public void setSeqNoPrimaryTerm(@Nullable SeqNoPrimaryTerm seqNoPrimaryTerm) {
			this.seqNoPrimaryTerm = seqNoPrimaryTerm;
		}
	}

	static class EntityWithListProperty {
		@Nullable
		@Id private String id;
		@Nullable private List<String> values;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public List<String> getValues() {
			return values;
		}

		public void setValues(@Nullable List<String> values) {
			this.values = values;
		}
	}

	static class EntityWithObject {
		@Nullable
		@Id private String id;
		@Nullable private Object content;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public Object getContent() {
			return content;
		}

		public void setContent(@Nullable Object content) {
			this.content = content;
		}
	}

	static class EntityWithNullField {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Text) private String notSaved;
		@Nullable
		@Field(type = FieldType.Text, storeNullValue = true) private String saved;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getNotSaved() {
			return notSaved;
		}

		public void setNotSaved(@Nullable String notSaved) {
			this.notSaved = notSaved;
		}

		@Nullable
		public String getSaved() {
			return saved;
		}

		public void setSaved(@Nullable String saved) {
			this.saved = saved;
		}
	}

	private static class ElectricCar extends Car {}

	private static class PersonWithCars {
		@Id
		@Nullable String id;
		@Field(type = FieldType.Text)
		@Nullable private String name;
		@Field(type = FieldType.Nested)
		@Nullable private List<? extends Car> cars;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public List<? extends Car> getCars() {
			return cars;
		}

		public void setCars(@Nullable List<Car> cars) {
			this.cars = cars;
		}
	}

	private static class EntityWithCustomValueConverters {
		@Nullable
		@Id private String id;
		@Nullable
		@ValueConverter(ClassBasedValueConverter.class) private String fieldWithClassBasedConverter;
		@Nullable
		@ValueConverter(EnumBasedValueConverter.class) private String fieldWithEnumBasedConverter;
		@Nullable private String dontConvert;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getFieldWithClassBasedConverter() {
			return fieldWithClassBasedConverter;
		}

		public void setFieldWithClassBasedConverter(@Nullable String fieldWithClassBasedConverter) {
			this.fieldWithClassBasedConverter = fieldWithClassBasedConverter;
		}

		@Nullable
		public String getFieldWithEnumBasedConverter() {
			return fieldWithEnumBasedConverter;
		}

		public void setFieldWithEnumBasedConverter(@Nullable String fieldWithEnumBasedConverter) {
			this.fieldWithEnumBasedConverter = fieldWithEnumBasedConverter;
		}

		@Nullable
		public String getDontConvert() {
			return dontConvert;
		}

		public void setDontConvert(@Nullable String dontConvert) {
			this.dontConvert = dontConvert;
		}
	}

	private static class ClassBasedValueConverter implements PropertyValueConverter {

		@Override
		public Object write(Object value) {
			return reverse(value);
		}

		@Override
		public Object read(Object value) {
			return reverse(value);
		}
	}

	private enum EnumBasedValueConverter implements PropertyValueConverter {
		INSTANCE;

		@Override
		public Object write(Object value) {
			return reverse(value);
		}

		@Override
		public Object read(Object value) {
			return reverse(value);
		}
	}

	private static class EntityWithCollections {
		@Field(type = FieldType.Keyword)
		@Nullable private List<String> stringList;

		@Field(type = FieldType.Keyword)
		@Nullable private Set<String> stringSet;

		@Field(type = FieldType.Object)
		@Nullable private List<Child> childrenList;

		@Field(type = FieldType.Object)
		@Nullable private Set<Child> childrenSet;

		@Nullable
		public List<String> getStringList() {
			return stringList;
		}

		public void setStringList(@Nullable List<String> stringList) {
			this.stringList = stringList;
		}

		@Nullable
		public Set<String> getStringSet() {
			return stringSet;
		}

		public void setStringSet(@Nullable Set<String> stringSet) {
			this.stringSet = stringSet;
		}

		@Nullable
		public List<Child> getChildrenList() {
			return childrenList;
		}

		public void setChildrenList(@Nullable List<Child> childrenList) {
			this.childrenList = childrenList;
		}

		@Nullable
		public Set<Child> getChildrenSet() {
			return childrenSet;
		}

		public void setChildrenSet(@Nullable Set<Child> childrenSet) {
			this.childrenSet = childrenSet;
		}

		public static class Child {

			@Field(type = FieldType.Keyword)
			@Nullable private String name;

			@Nullable
			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}
		}
	}

	private static final class ImmutableEntityWithCollections {
		@Field(type = FieldType.Keyword)
		@Nullable private List<String> stringList;

		@Field(type = FieldType.Keyword)
		@Nullable private Set<String> stringSet;

		@Field(type = FieldType.Object)
		@Nullable private List<Child> childrenList;

		@Field(type = FieldType.Object)
		@Nullable private Set<Child> childrenSet;

		public ImmutableEntityWithCollections(@Nullable List<String> stringList, @Nullable Set<String> stringSet,
				@Nullable List<Child> childrenList, @Nullable Set<Child> childrenSet) {
			this.stringList = stringList;
			this.stringSet = stringSet;
			this.childrenList = childrenList;
			this.childrenSet = childrenSet;
		}

		@Nullable
		public List<String> getStringList() {
			return stringList;
		}

		@Nullable
		public Set<String> getStringSet() {
			return stringSet;
		}

		@Nullable
		public List<Child> getChildrenList() {
			return childrenList;
		}

		@Nullable
		public Set<Child> getChildrenSet() {
			return childrenSet;
		}

		public static class Child {

			@Field(type = FieldType.Keyword)
			@Nullable private String name;

			public Child(@Nullable String name) {
				this.name = name;
			}

			@Nullable
			public String getName() {
				return name;
			}
		}
	}

	static class EntityWithPropertiesThatMightBeEmpty {
		@Nullable private String id;

		@Field(type = FieldType.Text)
		@Nullable private String stringToWriteWhenEmpty;

		@Field(type = FieldType.Text, storeEmptyValue = false)
		@Nullable private String stringToNotWriteWhenEmpty;

		@Field(type = FieldType.Nested)
		@Nullable private List<String> listToWriteWhenEmpty;

		@Field(type = FieldType.Nested, storeEmptyValue = false)
		@Nullable private List<String> listToNotWriteWhenEmpty;

		@Field(type = FieldType.Nested)
		@Nullable private Map<String, String> mapToWriteWhenEmpty;

		@Field(type = FieldType.Nested, storeEmptyValue = false)
		@Nullable private Map<String, String> mapToNotWriteWhenEmpty;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getStringToWriteWhenEmpty() {
			return stringToWriteWhenEmpty;
		}

		public void setStringToWriteWhenEmpty(@Nullable String stringToWriteWhenEmpty) {
			this.stringToWriteWhenEmpty = stringToWriteWhenEmpty;
		}

		@Nullable
		public String getStringToNotWriteWhenEmpty() {
			return stringToNotWriteWhenEmpty;
		}

		public void setStringToNotWriteWhenEmpty(@Nullable String stringToNotWriteWhenEmpty) {
			this.stringToNotWriteWhenEmpty = stringToNotWriteWhenEmpty;
		}

		@Nullable
		public List<String> getListToWriteWhenEmpty() {
			return listToWriteWhenEmpty;
		}

		public void setListToWriteWhenEmpty(@Nullable List<String> listToWriteWhenEmpty) {
			this.listToWriteWhenEmpty = listToWriteWhenEmpty;
		}

		@Nullable
		public List<String> getListToNotWriteWhenEmpty() {
			return listToNotWriteWhenEmpty;
		}

		public void setListToNotWriteWhenEmpty(@Nullable List<String> listToNotWriteWhenEmpty) {
			this.listToNotWriteWhenEmpty = listToNotWriteWhenEmpty;
		}

		@Nullable
		public Map<String, String> getMapToWriteWhenEmpty() {
			return mapToWriteWhenEmpty;
		}

		public void setMapToWriteWhenEmpty(@Nullable Map<String, String> mapToWriteWhenEmpty) {
			this.mapToWriteWhenEmpty = mapToWriteWhenEmpty;
		}

		@Nullable
		public Map<String, String> getMapToNotWriteWhenEmpty() {
			return mapToNotWriteWhenEmpty;
		}

		public void setMapToNotWriteWhenEmpty(@Nullable Map<String, String> mapToNotWriteWhenEmpty) {
			this.mapToNotWriteWhenEmpty = mapToNotWriteWhenEmpty;
		}
	}
	static class FieldNameDotsEntity {
		@Id
		@Nullable private String id;
		@Nullable
		@Field(value = "dotted.field", type = FieldType.Text) private String dottedField;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getDottedField() {
			return dottedField;
		}

		public void setDottedField(@Nullable String dottedField) {
			this.dottedField = dottedField;
		}
	}

	// endregion

	private static String reverse(Object o) {

		Assert.notNull(o, "o must not be null");

		return new StringBuilder().append(o.toString()).reverse().toString();
	}
}
