package me.msaproject.vehiclesimulatorservice;

import java.util.List;

public record RouteData(List<RoutePoint> points, double distanceMeters) {}