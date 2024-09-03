package SplunkIntegration.splunk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
public class Unzip {  
	private static String realZip;
   // public static void main( String[ ] args ) {  
       // String zipFilePath = "/users/rickromanelli/Documents/QuiltDump/new/R58RA0E8SFJ_21_01_28_219003_act.zip" ;  
    	//String zipFilePath = "/private/var/folders/qp/xg2f7gld5fj28hq4172ynwc00000gq/T/2156c6d6-da44-437a-a8ef-1a4917da40812568569966576290682.zip";
    	//String destDir = "/users/rickromanelli/Documents/QuiltDump/new/newer/newest" ;  
      // unzipfile( zipFilePath, destDir ) ;  
   // }  
    public static void unzipfile( String zipFilePath, String destDir ) {  
        File dir = new File( destDir ) ;  
        // creating an output directory if it doesn't exist already  
        if( !dir.exists( ) ) dir.mkdirs( ) ;  
        FileInputStream FiS ;  
        // buffer to read and write data in the file  
        byte[ ] buffer = new byte[ 1024 ] ;  
        try {  
            FiS = new FileInputStream( zipFilePath ) ;  
            ZipInputStream zis = new ZipInputStream( FiS ) ;  
            ZipEntry ZE = zis.getNextEntry( ) ;  
            System.out.println(ZE.getSize());
            while( ZE != null ) {  
                String fileName = ZE.getName( ) ;  
                File newFile = new File( destDir + File.separator + fileName ) ; 
                realZip = destDir + File.separator + fileName;
                System.out.println( " Unzipping to " + newFile.getAbsolutePath( ) ) ;  
                // create directories for sub directories in zip  
                new File( newFile.getParent( ) ).mkdirs( ) ;  
                FileOutputStream FoS = new FileOutputStream( newFile ) ;  
                int len ;  
                while ( ( len = zis.read( buffer ) )  > 0 ) {  
                	FoS.write( buffer, 0, len ) ;  
                }  
                FoS.close( ) ;  
                // close this ZipEntry  
                zis.closeEntry( ) ;  
                ZE = zis.getNextEntry( ) ;  
            }  
            // close last ZipEntry  
            zis.closeEntry( ) ;  
            zis.close( ) ;  
            FiS.close( ) ;  
        } catch ( IOException e ) {  
            e.printStackTrace( ) ;  
        }  
      }  
    public static void unzip(  String zipFilePath, String destDir ) {
    	unzipfile( zipFilePath, destDir );
    	unzipfile(realZip, destDir);
    }
}  