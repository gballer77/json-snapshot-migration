package software.baller.jsonsnapshotmigration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="jsonType", defaultImpl = AdultV1.class)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "v1", value = AdultV1.class),
        @JsonSubTypes.Type(name = "v2", value = AdultV2.class)
})
public class Adult {
    AdultV2 get(){
        if(this instanceof AdultV1 a) {
            return AdultV2.builder()
                    .id(a.id)
                    .name(a.name)
                    .age(a.age)
                    .ageInMonths(a.age * 12)
                    .children(a.children)
                    .build();
        } else if(this instanceof AdultV2) {
            return (AdultV2) this;
        } else {
            throw new IllegalArgumentException("Unknown child type: " + this.getClass().getName());
        }
    }
}
