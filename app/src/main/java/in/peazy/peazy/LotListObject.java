package in.peazy.peazy;

/**
 * Created by MB on 4/29/2017.
 */
public class LotListObject {
    String name;
    String stretch;
    String distance;
    String availability;

    public LotListObject(String name, String stretch, String distance, String availability ) {
        this.name=name;
        this.stretch=stretch;
        this.distance=distance;
        this.availability=availability;
    }

    public String getName() {
        return name;
    }

    public String getStretch() {
        return stretch;
    }

    public String getDistance() {
        return distance;
    }

    public String getAvailability() {
        return availability;
    }
}
