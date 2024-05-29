package software.baller.jsonsnapshotmigration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class Iteration0Tests {

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void jsonMarshall() throws JsonProcessingException {
        var childId = UUID.randomUUID();
        var childName = "Child";
        var childColor = "blue";

        ChildV1 child = new ChildV1(childId, childName, childColor);
        AdultV1 adult = new AdultV1(UUID.randomUUID(), "Adult", 29, List.of(child));

        Snapshot snapshot = new SnapshotV1(UUID.randomUUID(), 1, new Date(), adult);

        var jsonSnapshot = objectMapper.readValue(objectMapper.writeValueAsString(snapshot), SnapshotV1.class);

        assertEquals(jsonSnapshot.snapshot.children.get(0).id, childId);
        assertEquals(jsonSnapshot.snapshot.children.get(0).name, childName);
        assertEquals(jsonSnapshot.snapshot.children.get(0).favoriteColor, childColor);
    }
}
