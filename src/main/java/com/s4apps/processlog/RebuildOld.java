package com.s4apps.processlog;


/**
 *
 * @author mat
 */
public class RebuildOld {

        /**
         * @param args the command line arguments
         */
                public static void main(String[] args) {
                        System.out.println("Rebuilding the database (JPA) ...\n");
                        RebuildNew.main(args);
                }

}
