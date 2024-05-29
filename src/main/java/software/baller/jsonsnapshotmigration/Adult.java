package software.baller.jsonsnapshotmigration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="jsonType", defaultImpl = AdultV1.class)
@JsonSubTypes({@JsonSubTypes.Type(name = "v1", value = AdultV1.class)})
public class Adult {
    AdultV1 get(){
        return (AdultV1) this;
    }
}
