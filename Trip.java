package trip;

import graph.DirectedGraph;
import graph.LabeledGraph;
import graph.SimpleShortestPaths;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;


import static trip.Main.error;

/** Encapsulates a map containing sites, positions, and road distances between
 *  them.
 *  @author Mengyi Wang
 */
class Trip {

    /** Read map file named NAME into out map graph. */
    void readMap(String name) {
        int n;
        n = 0;
        try {
            File f = new File(name);
            Scanner inp = new Scanner(f);
            while (inp.hasNext()) {
                n += 1;
                switch (inp.next()) {
                case "L":
                    addLocation(inp.next(), inp.nextDouble(), inp.nextDouble());
                    break;
                case "R":
                    addRoad(inp.next(), inp.next(), inp.nextDouble(),
                            Direction.parse(inp.next()), inp.next());
                    break;
                default:
                    error("map entry #%d: unknown type", n);
                    break;
                }
            }
        } catch (InputMismatchException excp) {
            error("bad entry #%d", n);
        } catch (FileNotFoundException fnf) {
            error("cannot find file %s", name);
        } catch (NoSuchElementException excp) {
            error("entry incomplete at end of file");
        }
    }

    /** Produce a report on the standard output of a shortest journey from
     *  DESTS.get(0), then DESTS.get(1), .... */
    void makeTrip(List<String> dests) {
        if (dests.size() < 2) {
            error("must have at least two locations for a trip");
        }

        System.out.printf("From %s:%n%n", dests.get(0));
        int step;

        step = 1;
        for (int i = 1; i < dests.size(); i += 1) {
            Integer
                from = _sites.get(dests.get(i - 1)),
                to = _sites.get(dests.get(i));
            if (from == null) {
                error("No location named %s", dests.get(i - 1));
            } else if (to == null) {
                error("No location named %s", dests.get(i));
            }
            TripPlan plan = new TripPlan(from, to);
            plan.setPaths();
            List<Integer> segment = plan.pathTo(to);
            step = reportSegment(step, segment);
        }
    }

    /** Print out a written description of the location sequence SEGMENT,
     *  starting at FROM, and numbering the lines of the description starting
     *  at SEQ.
     *
     *  That is, FROM and each item in SEGMENT are the
     *  numbers of vertices representing locations.  Together, they
     *  specify the starting point and vertices along a path where
     *  each vertex is joined to the next by an edge. where is the edge
     * @param segment is a list of integers.
     * @param seq is a swq
     * @return an interger */


    int reportSegment(int seq, List<Integer> segment) {
        int roundingcConstant = 10;
        Iterator<Integer> iter = segment.iterator();
        int first = iter.next();
        int second = iter.next();
        Road firstrRoad = _map.getLabel(first, second);
        double firstDist = firstrRoad.length();
        String firstName = firstrRoad.toString();
        String firstdDirection = firstrRoad.direction().fullName();
        double secondDist = 0;
        String secondName = new String();
        String secondDirection = new String();
        while (iter.hasNext()) {
            first = second;
            second = iter.next();
            Road secondRoad = _map.getLabel(first, second);
            secondDist = secondRoad.length();
            secondName = secondRoad.toString();
            secondDirection = secondRoad.direction().fullName();
            if (secondDirection.equals(firstdDirection)
                    && secondName.equals(firstName)) {
                firstDist += secondDist;
            } else {
                System.out.println(seq + ". Take " + firstName + " "
                        + firstdDirection + " for "
                        + (double) Math.round(firstDist
                        * roundingcConstant) / roundingcConstant + " miles.");
                firstName = secondName;
                firstdDirection = secondDirection;
                firstDist = secondDist;
                seq += 1;
            }
        }
        System.out.println(seq + ". Take " + secondName + " "
                + secondDirection + " for "
                + (double) Math.round(firstDist
                * roundingcConstant) / roundingcConstant
                + " miles " + "to "
                + _map.getLabel(second).toString() + ".");
        seq += 1;
        return seq;
    }

    /** Add a new location named NAME at (X, Y). */
    private void addLocation(String name, double x, double y) {
        if (_sites.containsKey(name)) {
            error("multiple entries for %s", name);
        }
        int v = _map.add(new Location(name, x, y));
        _sites.put(name, v);
    }

    /** Add a stretch of road named NAME from the Location named FROM
     *  to the location named TO, running in direction DIR, and
     *  LENGTH miles long.  Add a reverse segment going back from TO
     *  to FROM. */
    private void addRoad(String from, String name, final double length,
                         Direction dir, String to) {
        Integer v0 = _sites.get(from),
            v1 = _sites.get(to);

        if (v0 == null) {
            error("location %s not defined", from);
        } else if (v1 == null) {
            error("location %s not defined", to);
        }
        Road pathTo = new Road(name, dir, length);
        _map.add(v0, v1, pathTo);
        Road pathBack = new Road(name, dir.reverse(), length);
        _map.add(v1, v0, pathBack);
    }

    /** Represents the network of Locations and Roads. */
    private RoadMap _map = new RoadMap();
    /** Mapping of Location names to corresponding map vertices. */
    private HashMap<String, Integer> _sites = new HashMap<>();

    /** A labeled directed graph of Locations whose edges are labeled by
     *  Roads. */
    private static class RoadMap extends LabeledGraph<Location, Road> {
        /** An empty RoadMap. */
        RoadMap() {
            super(new DirectedGraph());
        }
    }

    /** Paths in _map from a given location. */
    private class TripPlan extends SimpleShortestPaths {
        /** A plan for travel from START to DEST according to _map. */
        TripPlan(int start, int dest) {
            super(_map, start, dest);
            _finalLocation = _map.getLabel(dest);
        }

        /** return a distance / weight of an edge.
         * @param u a vertex
         * @param v a vertex
         * @return a double variable */
        protected double getWeight(int u, int v) {
            if (!_G.contains(u, v)) {
                return Double.MAX_VALUE;
            } else {
                return _map.getLabel(u, v).length();
            }

        }

        @Override
        protected double estimatedDistance(int v) {
            return _map.getLabel(v).dist(_finalLocation);
        }

        /** Location of the destination. */
        private final Location _finalLocation;

    }

}
