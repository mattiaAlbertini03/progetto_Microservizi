package me.msaproject.simulatorservice.dto;

import java.util.List;

public record RouteData(List<RoutePoint> points, double distanceMeters) {}