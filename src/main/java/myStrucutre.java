import java.util.*;

public final class myStrucutre {


    // This is my own data structure I am using to implement the key value for HASH MAP and HASH TABLES. I want keys to be equal when the string representing the date and province are the same.
    // So I had to override hashcode and equals to make sure of this.

    private final String date;
    private final String province;

    public myStrucutre(String date, String province) {
        this.date = date;
        this.province = province;

    }

    public String getDate() {
        return date;
    }

    public String getProvince() {
        return province;
    }

    @Override // Want to make sure the Strings are equal then the keys are equal
    public int hashCode() {
        int hash = 0 ;
        hash = 89 * hash + (date != null ? date.hashCode() : 0);
        hash = 89 * hash + (province != null ? province.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        myStrucutre thing = (myStrucutre) o;

        if (province != null ? !province.equals(thing.province) : thing.province != null) return false;

        return date != null ? date.equals(thing.date) : thing.date == null;
    }
}

