package fedora.server.resourceIndex;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import fedora.server.storage.types.DigitalObject;

/**
 * @author Edwin Shin
 */
public class TestResourceIndexDB extends TestResourceIndex {
    private DigitalObject bdef, bmech, dataobject;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestResourceIndexDB.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        
        bdef = getDigitalObject(new File(DEMO_OBJECTS_ROOT_DIR
                + "/bdefs/demo_ri8.xml"));
        bmech = getDigitalObject(new File(DEMO_OBJECTS_ROOT_DIR
                + "/bmechs/demo_ri9.xml"));
        dataobject = getDigitalObject(new File(
                DEMO_OBJECTS_ROOT_DIR + "/dataobjects/demo_ri10.xml"));
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testDBCache() throws Exception {
        m_ri.addDigitalObject(bdef);
        m_ri.commit();
        
        Connection conn = m_cPool.getConnection();
        Statement select = conn.createStatement();
        
        String query = "SELECT * FROM riMethod";
        ResultSet rs = select.executeQuery(query);
        rs.last();
        assertEquals(1, rs.getRow());
        
        query = "SELECT * FROM riMethodPermutation";
        rs = select.executeQuery(query);
        rs.last();
        assertEquals(1, rs.getRow());
        
        m_ri.deleteDigitalObject(bdef);
        m_ri.commit();
        
        query = "SELECT * FROM riMethod";
        rs = select.executeQuery(query);
        rs.last();
        assertEquals(0, rs.getRow());
        
        query = "SELECT * FROM riMethodPermutation";
        rs = select.executeQuery(query);
        rs.last();
        assertEquals(0, rs.getRow());
        
        m_ri.addDigitalObject(bmech);
        m_ri.commit();
        
        query = "SELECT * FROM riMethodImpl";
        rs = select.executeQuery(query);
        rs.last();
        assertEquals(2, rs.getRow());
        
        query = "SELECT * FROM riMethodMimeType";
        rs = select.executeQuery(query);
        rs.last();
        assertEquals(3, rs.getRow());
        
        m_ri.deleteDigitalObject(bmech);
        m_ri.commit();
        
        query = "SELECT * FROM riMethodImpl";
        rs = select.executeQuery(query);
        rs.last();
        assertEquals(0, rs.getRow());
        
        query = "SELECT * FROM riMethodMimeType";
        rs = select.executeQuery(query);
        rs.last();
        assertEquals(0, rs.getRow());
    }
}