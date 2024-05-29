package software.baller.jsonsnapshotmigration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


public class ExperimentTests {

    ObjectMapper objectMapper = new ObjectMapper();
    UUID adultId = UUID.randomUUID();
    String adultName = "Adult";
    Integer adultAge = 29;

    UUID childId = UUID.randomUUID();
    String childName = "Child";
    String childColor = "blue";

    @Test
    void v1() throws JsonProcessingException {
        ChildV1 child = new ChildV1(childId, childName, childColor);
        AdultV1 adult = new AdultV1(adultId, adultName, adultAge, List.of(child));

        Snapshot snapshot = new SnapshotV1(UUID.randomUUID(), 1, new Date(), adult);

        var jsonOut = objectMapper.writeValueAsString(snapshot);

        var jsonIn = objectMapper.readValue(jsonOut, Snapshot.class);

        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().id, childId);
        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().name, childName);
        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().favoriteColor, childColor);
    }

    @Test
    void childV2() throws JsonProcessingException {
        ChildV2 child = new ChildV2(childId, childName, childColor, 2);
        AdultV1 adult = new AdultV1(adultId, adultName, adultAge, List.of(child));

        Snapshot snapshot = new SnapshotV1(UUID.randomUUID(), 1, new Date(), adult);

        var jsonOut = objectMapper.writeValueAsString(snapshot);

        var jsonIn = objectMapper.readValue(jsonOut, Snapshot.class);

        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().id, childId);
        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().name, childName);
        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().favoriteColor, childColor);
        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().age, 2);
    }

    @Test
    void childV2_upconvertsFromV1() throws JsonProcessingException {
        ChildV1 child = new ChildV1(childId, childName, childColor);
        AdultV1 adult = new AdultV1(adultId, adultName, adultAge, List.of(child));

        Snapshot snapshot = new SnapshotV1(UUID.randomUUID(), 1, new Date(), adult);

        var jsonOut = objectMapper.writeValueAsString(snapshot);

        var jsonIn = objectMapper.readValue(jsonOut, Snapshot.class);

        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().id, childId);
        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().name, childName);
        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().favoriteColor, childColor);
        assertNull(jsonIn.get().snapshot.get().children.get(0).get().age);
    }

    @Test
    void adultV2() throws JsonProcessingException {
        ChildV2 child = new ChildV2(childId, childName, childColor, 2);
        AdultV2 adult = new AdultV2(adultId, adultName, adultAge, 400, List.of(child));

        Snapshot snapshot = new SnapshotV1(UUID.randomUUID(), 1, new Date(), adult);

        var jsonOut = objectMapper.writeValueAsString(snapshot);

        var jsonIn = objectMapper.readValue(jsonOut, Snapshot.class);

        assertEquals(jsonIn.get().snapshot.get().id, adultId);
        assertEquals(jsonIn.get().snapshot.get().name, adultName);
        assertEquals(jsonIn.get().snapshot.get().age, adultAge);
        assertEquals(jsonIn.get().snapshot.get().ageInMonths, 400);

        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().id, childId);
        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().name, childName);
        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().favoriteColor, childColor);
        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().age, 2);
    }

    @Test
    void adultV2_upconvertsFromV1() throws JsonProcessingException {
        ChildV2 child = new ChildV2(childId, childName, childColor, 2);
        AdultV1 adult = new AdultV1(adultId, adultName, adultAge, List.of(child));

        Snapshot snapshot = new SnapshotV1(UUID.randomUUID(), 1, new Date(), adult);

        var jsonOut = objectMapper.writeValueAsString(snapshot);

        var jsonIn = objectMapper.readValue(jsonOut, Snapshot.class);

        assertEquals(jsonIn.get().snapshot.get().id, adultId);
        assertEquals(jsonIn.get().snapshot.get().name, adultName);
        assertEquals(jsonIn.get().snapshot.get().age, adultAge);
        assertEquals(jsonIn.get().snapshot.get().ageInMonths, adultAge * 12);

        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().id, childId);
        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().name, childName);
        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().favoriteColor, childColor);
        assertEquals(jsonIn.get().snapshot.get().children.get(0).get().age, 2);
    }
}
