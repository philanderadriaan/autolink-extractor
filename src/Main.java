import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

public class Main
{

  public static void main(String[] args) throws Exception
  {
    Properties properties = new Properties();
    properties.load(new FileInputStream("properties.properties"));
    File sourceDirectory = new File(properties.getProperty("source"));
    File destinationDirectory = new File(properties.getProperty("destination"));

    Set<String> exceptionSet = new HashSet<String>();
    Set<String> skipSet = new HashSet<String>();
    for (File sourceFile : sourceDirectory.listFiles())
    {
      skipSet.add(sourceFile.getName());
    }

    List<String> nameList =
        Files.readAllLines(Paths.get(destinationDirectory.getAbsolutePath() + "\\NAME.txt"));
    Collections.shuffle(nameList);

    for (String name : nameList)
    {
      for (File characterDirectory : destinationDirectory.listFiles())
      {
        if (characterDirectory.isDirectory() && characterDirectory.getName().equals(name))
        {
          LogUtility.log("Delete " + characterDirectory.getAbsolutePath());
          characterDirectory.delete();
        }
      }
      for (File archiveFile : sourceDirectory.listFiles())
      {
        if (archiveFile.isFile() &&
            archiveFile.getName().toUpperCase().startsWith(name.substring(0, 3).toUpperCase()))
        {
          try
          {
            if (extract(archiveFile, destinationDirectory.getAbsolutePath(), name, 1))
            {
              extract(archiveFile, destinationDirectory.getAbsolutePath(), name, 2);
            }
          }
          catch (Exception e)
          {
            exceptionSet.add(e.getMessage());
            e.printStackTrace();
          }
          skipSet.remove(archiveFile.getName());
        }
      }
    }
    for (String skip : skipSet)
    {
      LogUtility.log("Skip " + skip);
    }
    for (String exception : exceptionSet)
    {
      LogUtility.log(exception);
    }
    AudioUtility.flush();
  }

  private static boolean extract(File archiveFile, String destinationDirectoryPath,
                                 String name, int sequence)
      throws Exception
  {
    File characterDirectory = new File(destinationDirectoryPath + "\\" + name + "\\" +
                                       String.format("%02d", sequence));
    Files.createDirectories(Paths.get(characterDirectory.getAbsolutePath()));
    LogUtility.log("Extract " + archiveFile.getAbsolutePath() + " to " +
                   characterDirectory.getAbsolutePath());
    boolean hasPhys = false;
    SevenZFile sevenZFile = new SevenZFile(archiveFile);
    SevenZArchiveEntry sevenZArchiveEntry = sevenZFile.getNextEntry();
    while (sevenZArchiveEntry != null)
    {
      if (sequence == 1 || !sevenZArchiveEntry.getName().toUpperCase().endsWith(".---C"))
      {
        LogUtility.log(sevenZArchiveEntry.getName());
        FileOutputStream fileOutputStream =
            new FileOutputStream(characterDirectory + "\\" + sevenZArchiveEntry.getName());
        byte[] bytes = new byte[(int) sevenZArchiveEntry.getSize()];
        sevenZFile.read(bytes, 0, bytes.length);
        fileOutputStream.write(bytes);
        fileOutputStream.close();
        if (sevenZArchiveEntry.getName().toUpperCase().endsWith(".PHYD"))
        {
          hasPhys = true;
        }
      }
      sevenZArchiveEntry = sevenZFile.getNextEntry();
    }
    sevenZFile.close();
    return hasPhys;
  }
}
