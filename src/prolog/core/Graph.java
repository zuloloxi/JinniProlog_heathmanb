package prolog.core;

import java.util.Arrays;
import static java.util.Arrays.stream;
import prolog.kernel.*;
import prolog.logic.*;
import static prolog.logic.Interact.printStackTrace;
import static prolog.logic.Prolog.dump;

/**
 * Implements efficient Directed Graphs supporting constant time access to
 * vertices, in and out edges, and associated data. Given that Graph is based on
 * a unique integer being associated to each vertex, it is possible, once the
 * Graph is stable and no more deletions occur, to run graph algorithms directly
 * on the supporting arrays - which should improve their performance by 1-2
 * orders of magnitude as array access is about 50 times faster in Java than
 * hash table access.
 */
public class Graph extends ObjectDict {

    /**
     * Creates a Graph with a given number of vertices.
     * @param size
     */
    public Graph(int size) {
        super(size);
    }

    /**
     * Creates a new Graph. The set of vertices and edges expands shrinks
     * dynamically.
     */
    public Graph() {
        super();
    }

    private int edgeCount;

    /**
     *
     * @return
     */
    public int getEdgeCount() {
        return edgeCount;
    }

    /**
     *
     * @param v
     * @return
     */
    protected Content toVertex(Object v) {
        return toVertex(v, null);
    }

    /**
     * Makes a vertex associated to v, unless it already exists
     * @param v
     * @param data
     * @return 
     */
    protected final Content toVertex(Object v, Object data) {
        Content p = (Content) get(v);
        if (null == p) {
            p = new Content(new ObjectDict(), new ObjectDict(), wrapData(data));
            addNewEntry(v, p);
        }
        return p;
    }

    /**
     *
     * @param data
     * @return
     */
    protected Object wrapData(Object data) {
        return data;
    }

    /**
     * Creates a new Vertex associated to and object v in this graphs and
     * initializes its data (possibly to null), unless the vertex already
     * exists, in which case this command has no effect. This means that v acts
     * as a unique key identifying the vertex and that its associated data is
     * "final", i.e it can only be set at definition time. If data added to
     * nodes is important, vertices have to be created before edges. Creating
     * edges first is ok if they are designed to contain vertices with empty
     * data. As associated data is designed to be final, the only way to change
     * it is by calling its methods and update its fields.
     * @param v
     * @param data
     */
    public void addVertex(Object v, Object data) {
        toVertex(v, data);
    }

    /**
     * Returns a lightweight Iterator on vertices. We assume no changes happen
     * while the iterator is working.
     * @return 
     */
    public ObjectIterator vertexIterator() {
        return getKeys();
    }

    /**
     * Returns the number of outgoing edges
     * @param v
     * @return 
     */
    public int outDegree(Object v) {
        Content p = (Content) get(v);
        if (null == p) {
            return -1;
        }
        ObjectDict fs = (ObjectDict) p.key;
        return fs.size();
    }

    /**
     * Returns an iterator over outgoing edges for a vertex.
     * @param v
     * @return 
     */
    public ObjectIterator outIterator(Object v) {
        Content p = (Content) get(v);
        if (null == p) {
            return null;
        }
        ObjectDict fs = (ObjectDict) p.key;
        return fs.getKeys();
    }

    /**
     * Returns the number of incoming edges
     * @param v
     * @return 
     */
    public int inDegree(Object v) {
        Content p = (Content) get(v);
        if (null == p) {
            return -1;
        }
        ObjectDict ts = (ObjectDict) p.value;
        return ts.size();
    }

    /**
     * Returns an iterator over incoming edges for a vertex.
     * @param v
     * @return 
     */
    public ObjectIterator inIterator(Object v) {
        Content p = (Content) get(v);
        if (null == p) {
            return null;
        }
        ObjectDict ts = (ObjectDict) p.value;
        return ts.getKeys();
    }

    /**
     *
     */
    public void dualize() {
        ObjectIterator Vs = vertexIterator();
        while (Vs.hasNext()) {
            Object v = Vs.next();
            Content p = (Content) get(v);
            if (null == p) {
                continue;
            }
            ObjectDict fs = (ObjectDict) p.key;
            ObjectDict ts = (ObjectDict) p.value;
            p.key = ts;
            p.value = fs;
        }
    }

    /*
    TODO: Dead code.
     Updates Vertex data, assuming the vertex exists.
     Not needed anymore, as Data is Final.
  
     public boolean setVertexData(Object v,Object data) {
     Content p=(Content)get(v);
     if(null==p) return false;
     p.data=data;
     return true;
     }
     */
    /**
     * Retrieves the raw data object associated to a vertex. For better
     * performance, it is a good idea to update it in place, by reference when
     * possible.
     * @param v
     * @return 
     */
    public Object rawVertexData(Object v) {
        Content p = (Content) get(v);
        if (null == p) {
            return null;
        }
        return p.data;
    }

    /**
     * Retrieves the possibly wrapped data object associated to a vertex. For
     * better performance, it is a good idea to update it in place, by reference
     * when possible.
     * @param v
     * @return 
     */
    public Object vertexData(Object v) {
        return rawVertexData(v);
    }

    int filter = DATA;
    String sfilter = "data"; // was null ???

    /**
     * indicates focus of attribute filter on KEY
     */
    public static final int KEY = 0;
    /**
     * indicates focus of attribute filter on DATA
     */
    public static final int DATA = 1;
    /**
     * indicates focus of attribute filter on ID
     */
    public static final int ID = 2;
    /**
     * indicates focus of attribute filter on RANK
     */
    public static final int RANK = 3;
    /**
     * indicates focus of attribute filter on named attribute of a vertex
     * managed by setProperty/getProperty
     */
    public static final int ATTR = 4;

    /**
     * sets filter to KEY,DATA,ID,RANK,ATTR for use by getSelected
     * @param sfilter
     */
    public void setSelectFilter(String sfilter) {
        if (null != sfilter) {
            switch (sfilter) {
                case "key":
                    filter = KEY;
                    break;
                case "data":
                    filter = DATA;
                    break;
                case "id":
                    filter = ID;
                    break;
                case "rank":
                    filter = RANK;
                    break;
                default:
                    filter = ATTR;
                    this.sfilter = sfilter;
                    break;
            }
        }
    }

    /**
     * gets data selected by filter
     * @param v
     * @return 
     */
    public Object getSelected(Object v) {
        Object val;
        switch (this.filter) {
            case KEY:
                val = v;
                break;
            case DATA:
                val = vertexData(v);
                break;
            case ID:
                val = getOrdinal(v);
                break;
            default:
                val = null;
        }
        return val;
    }

    /**
     * Retrieves the data object associated to an edge. For better performance,
     * data if final - it is usually a container supporting in-place updates.
     * @param from
     * @param to
     * @return 
     */
    public Object edgeData(Object from, Object to) {
        Content p = (Content) get(from);
        if (null == p) {
            return null;
        }
        ObjectDict fs = (ObjectDict) p.key;
        return fs.get(to);
    }

    /*
    TODO: Dead code.
     Updates edge data, assuming the edge exists. Not needed
     as Data is final!!!
  
     public boolean setEdgeData(Object from,Object to,Object data) {
     Content p=(Content)get(from);
     if(null==p) return false;
     ObjectDict fs=(ObjectDict)p.key;
     fs.put(to,data);
     p=(Content)get(to);
     ObjectDict ts=(ObjectDict)(p.value);
     ts.put(from,data);
     return true;
     }
     */
    /**
     * Adds an edge to the graph. Note that it (possibly) initializes missing
     * vertexes with null data, unless they have been initialized in advance.
     * This means that if one wants vertices to contain useful data, they will
     * have to be created before edges.
     * @param from
     * @param to
     * @param data
     */
    public void addEdge(Object from, Object to, Object data) {
        ObjectDict fs = (ObjectDict) (toVertex(from).key);
        if (null == fs.put(to, data)) {
            edgeCount++;
        }
        ObjectDict ts = (ObjectDict) (toVertex(to).value);
        ts.put(from, data);
    }

    /**
     * Removes an edge.
     * @param from
     * @param to
     * @return 
     */
    public boolean removeEdge(Object from, Object to) {
        Content p = (Content) get(from);
        if (null == p) {
            return false;
        }
        ObjectDict fs = (ObjectDict) (p.key);
        if (null != fs.remove(to)) {
            edgeCount--;
        }
        p = (Content) get(to);
        if (null == p) {
            return true;
        }
        ObjectDict ts = (ObjectDict) (p.value);
        ts.remove(from);
        return true;
    }

    /**
     * Removes a vertex and all edges from and to the removed vertex.
     * @param v
     * @return 
     */
    public boolean removeVertex(Object v) {
        Content p = (Content) get(v);
        if (null == p) {
            return false;
        }
        ObjectDict fs = (ObjectDict) p.key;
        Object[] ks = fs.toKeys();
        stream(ks).parallel().forEach(k -> removeEdge(v,k));
        ObjectDict ts = (ObjectDict) p.value;
        ks = ts.toKeys();
        stream(ks).parallel().forEach(k -> removeEdge(k,v));
        remove(v);
        return true;
    }

    /**
     * Returns a more verbose String representation that exhibits the details of
     * component Maps.
     * @return 
     */
    public String asMap() {
        return super.toString();
    }

    /**
     *
     * @param F
     * @return
     */
    public Object visit(GraphVisitor F) {
        F.init();
        ObjectIterator Vs = getKeys();
        while (Vs.hasNext()) {
            Object V = Vs.next();
            if (F.isVisited(V)) {
                continue;
            }
            F.start();
            recVisit(F, V);
            F.stop();
        }
        return F.end();
    }

    private void recVisit(GraphVisitor F, Object V) {
        F.visit(V);
        if (F.visitIn()) {
            ObjectIterator Vs = inIterator(V);
            while (Vs.hasNext()) {
                Object U = Vs.next();
                if (F.isVisited(U)) {
                    continue;
                }
                recVisit(F, U);
            }
        }
        if (F.visitOut()) {
            ObjectIterator Vs = outIterator(V);
            while (Vs.hasNext()) {
                Object U = Vs.next();
                if (F.isVisited(U)) {
                    continue;
                }
                recVisit(F, U);
            }
        }
        F.unvisit(V);
    }

    /**
     * Returns a string representation of this Graph.
     * @return 
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        ObjectIterator I = getKeys();

        while (I.hasNext()) {
            Object k = I.next();
            buf.append("[").append(getOrdinal(k)).append("]");
            buf.append(k);
            Content p = (Content) get(k);
            if (null != p.data) {
                buf.append(":").append(p.data);
            }
            buf.append("=>");
            {
                ObjectIterator J = ((ObjectDict) p.key).getKeys();
                if (J.hasNext()) {
                    buf.append(showEdge(k, J.next()));
                }
                while (J.hasNext()) {
                    buf.append(",");
                    buf.append(showEdge(k, J.next()));
                }
            }
            buf.append("\n");
        }
        buf.append("\n");
        return buf.toString();
    }

    private String showEdge(Object k, Object OutV) {
        Object D = edgeData(k, OutV);
        String s = (null == D) ? "" : D.toString();
        return "<" + s + ">:" + OutV;
    }

    /**
     *
     */
    public static void gtest() {
        try {
            Graph G = new Graph();
            G.addEdge("a", "b", "11");
            G.addEdge("a", "c", "22");
            G.addEdge("c", "a", "33");
            G.addEdge("b", "b", "44");
            G.addEdge("b", "d", "55");
            G.addEdge("d", "a", "66");
            G.addVertex("e", "77");
            G.addEdge("a", "d", "88");
            G.addEdge("c", "c", "99");
            ObjectIterator Vs = G.vertexIterator();
            while (Vs.hasNext()) {
                Object V = Vs.next();
                dump(V + ": in=" + G.inDegree(V) + " out=" + G.outDegree(V));
            }
            dump("\n" + G.toString());
            dump("super:\n" + G.asMap());
            dump("\nIGraph Test\n");
            IGraph IG = new IGraph(G, null);
            dump("\nIGraph\n" + IG);

            G.removeEdge("c", "a");
            G.removeVertex("b");
            dump("c=>a and b removed:\n" + G.toString());
            dump(G.info() + "edges=" + G.getEdgeCount());
            G.removeVertex("d");
            dump("d removed:\n" + G.toString());
            dump(G.info() + "edges=" + G.getEdgeCount());
            G.shuffle(1001);
            dump("\nafter compacting and shuffling\n" + G);
            dump(G.info() + "edges=" + G.getEdgeCount());
        } catch (Exception e) {
            printStackTrace(e);
        }
    }
}
