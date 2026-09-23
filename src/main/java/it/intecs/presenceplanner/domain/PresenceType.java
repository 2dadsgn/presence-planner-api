package it.intecs.presenceplanner.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The value a day can be assigned. Ids match the frontend's PRESENCE_TYPES
 * (src/app/core/models/presence.model.ts) exactly, so no mapping layer is
 * needed between the two.
 */
public enum PresenceType {
  OFFICE("office"),
  REMOTE("remote"),
  VACATION("vacation"),
  SICK("sick"),
  OFF("off");

  private final String id;

  PresenceType(String id) {
    this.id = id;
  }

  @JsonValue
  public String id() {
    return id;
  }

  @JsonCreator
  public static PresenceType fromId(String id) {
    for (PresenceType t : values()) {
      if (t.id.equals(id)) return t;
    }
    throw new IllegalArgumentException("Unknown presence type: " + id);
  }
}
