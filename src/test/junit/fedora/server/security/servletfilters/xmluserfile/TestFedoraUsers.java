package fedora.server.security.servletfilters.xmluserfile;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.junit.Test;

public class TestFedoraUsers {

	@Test
	public void testGetInstance() throws Exception {
		FedoraUsers fu = FedoraUsers.getInstance();
		assertNotNull(fu);
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		fu.write(outputWriter);
		outputWriter.close();
	}

	@Test
	public void testGetInstanceString() throws Exception {
	    String fedoraUsersXML = "src/fcfg/server/fedora-users.xml";
	    File f = new File(fedoraUsersXML);
	    System.out.println(f.toURI().toString());
	    FedoraUsers fu = FedoraUsers.getInstance(f.toURI());
	    assertNotNull(fu);
	    Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
	    fu.write(outputWriter);
        outputWriter.close();
	}
	
/*

	@Test
	public void testGetRoles() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetUsers() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddRole() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddUser() {
		fail("Not yet implemented");
	}

	@Test
	public void testWrite() {
		fail("Not yet implemented");
	}
*/

}
