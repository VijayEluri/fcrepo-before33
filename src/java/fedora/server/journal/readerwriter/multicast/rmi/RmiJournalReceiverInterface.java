
package fedora.server.journal.readerwriter.multicast.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import fedora.server.journal.JournalException;

/**
 * The RMI proxy interface for the {@link RmiJournalReceiver}.
 * 
 * @author Jim Blake
 */
public interface RmiJournalReceiverInterface
        extends Remote {

    String RMI_BINDING_NAME = "RmiJournalReceiver";

    void openFile(String repositoryHash, String filename)
            throws RemoteException, JournalException;

    void writeText(String indexedHash, String text) throws RemoteException,
            JournalException;

    void closeFile() throws RemoteException, JournalException;
}