/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.messaging;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Date;

import fedora.common.PID;
import fedora.server.Context;

/**
 * Representation of a Fedora API Method.
 * 
 * @author Edwin Shin
 * @version $Id$
 */
public class FedoraMethod {
    private Method method;
    private Object[] args;
    private Object returnVal;
    
    private String methodName;
    private PID pid;
    private Context context;
    private String[] parameterNames;
    private Date date;
    
    public FedoraMethod(Method method, Object[] args, Object returnVal) {
        this.method = method;
        this.args = args;
        this.returnVal = returnVal;
        
        methodName = method.getName();
    }
    
    public Method getMethod() {
        return method;
    }
    
    public Object[] getParameters() {
        return args;
    }
    
    /**
     * Get the return value of the API method invocation.
     * @return The return value or <code>null</code>.
     */
    public Object getReturnValue() {
        return returnVal;
    }
    
    public String getName() {
        return methodName;
    }
    
    /**
     * Get the <code>Context</code> of the API method.
     * @return The <code>Context</code> or <code>null</code>.
     */
    public Context getContext() {
        if (context == null) {
            String[] pNames = getParameterNames();
            for (int i = 0; i < pNames.length; i++) {
                if (pNames[i].equals("context")) {
                    context = (Context)args[i];
                }
            }
        }
        return context;
    }
    
    /**
     * Get the <code>PID</code> of this method.
     * @return The <code>PID</code> or null.
     */
    public PID getPID() {
        if (pid == null) {
            // The pid of Management.ingest is provided by the return value
            if (methodName.equals("ingest")) {
                pid = PID.getInstance((String)returnVal);
            } else {
                String[] pNames = getParameterNames();
                for (int i = 0; i < pNames.length; i++) {
                    if (pNames[i].equals("pid")) {
                        pid = PID.getInstance((String)args[i]);
                    }
                }
            }
        }
        return pid;
    }
    
    /**
     * Get the <code>Date</code> of the API method invocation.
     * @return The <code>Date</code> or <code>null</code> if not available.
     */
    public Date getDate() {
        if (date == null) {
            if (getContext() != null) {
                date = context.now();
            }
        }
        return date;
    }
    
    public String[] getParameterNames() {
        if (parameterNames == null) {
            parameterNames = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                parameterNames[i] = getParameterName(method, i);
            }
        }
        return parameterNames;
    }
    
    private static <A extends Annotation> A getParameterAnnotation(Method m,
                                                           int paramIndex,
                                                           Class<A> annot) {
        for (Annotation a : m.getParameterAnnotations()[paramIndex]) {
            if (annot.isInstance(a))
                return annot.cast(a);
        }
        return null;
    }
    
    private String getParameterName(Method m, int paramIndex) {
        PName pName = getParameterAnnotation(m, paramIndex, PName.class);
        if (pName != null) {
            return pName.value();
        } else {
            return "";
        }
    }
}
