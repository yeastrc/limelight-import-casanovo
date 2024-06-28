package org.yeastrc.limelight.xml.casanovo.objects;

import java.math.BigDecimal;
import java.util.Objects;

public class OpenModification {

    private BigDecimal mass;
    private Integer position;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpenModification that = (OpenModification) o;
        return mass.equals(that.mass) && Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mass, position);
    }

    @Override
    public String toString() {
        return "OpenModification{" +
                "mass=" + mass +
                ", position=" + position +
                '}';
    }

    public BigDecimal getMass() {
        return mass;
    }

    public Integer getPosition() {
        return position;
    }

    public OpenModification(BigDecimal mass, Integer position) {
        this.mass = mass;
        this.position = position;
    }
}
