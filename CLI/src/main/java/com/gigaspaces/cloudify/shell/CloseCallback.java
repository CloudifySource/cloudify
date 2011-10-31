package com.gigaspaces.cloudify.shell;

import org.apache.felix.service.command.CommandSession;

/**
 * @author uri
 */
class CloseCallback implements Runnable {

    private CommandSession session;


    public void run() {
        AdminFacade adminFacade = (AdminFacade) session.get(Constants.ADMIN_FACADE);
        try {
            adminFacade.disconnect();

            if (session.get(Constants.INTERACTIVE_MODE) != null) {
                boolean isInteractive = (Boolean)session.get(Constants.INTERACTIVE_MODE);
                if (!isInteractive) {
                    if (session.get(Constants.LAST_COMMAND_EXCEPTION) != null) {
//                        Throwable t = (Throwable) session.get(Constants.LAST_COMMAND_EXCEPTION);
                        System.exit(1);
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void setSession(CommandSession session) {
        this.session = session;
    }
}
