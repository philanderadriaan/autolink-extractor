import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

public class Main
{

  public static void main(String[] args) throws Exception
  {
    Properties properties = new Properties();
    properties.load(new FileInputStream("Properties.properties"));
    File sourceDirectory = new File(properties.getProperty("source"));
    File destinationDirectory = new File(properties.getProperty("destination"));

    Set<String> skipSet = new TreeSet<String>();
    for (File sourceFile : sourceDirectory.listFiles())
    {
      skipSet.add(sourceFile.getName());
    }

    Files.lines(Paths.get(destinationDirectory.getAbsolutePath() + "\\NAME.txt"))
        .forEach(name -> {
          try
          {
            for (File characterDirectory : destinationDirectory.listFiles())
            {
              if (characterDirectory.isDirectory() &&
                  characterDirectory.getName().equals(name))
              {
                log("Delete " + characterDirectory.getAbsolutePath());
                characterDirectory.delete();
              }
            }
            for (File archiveFile : sourceDirectory.listFiles())
            {
              if (archiveFile.isFile() && archiveFile.getName().toUpperCase()
                  .startsWith(name.substring(0, 3).toUpperCase()))
              {
                if (extract(archiveFile, destinationDirectory.getAbsolutePath(), name, 1))
                {
                  extract(archiveFile, destinationDirectory.getAbsolutePath(), name, 2);
                }
                skipSet.remove(archiveFile.getName());
              }
            }
          }
          catch (Exception exception)
          {
            exception.printStackTrace();
          }
        });
    for (String skip : skipSet)
    {
      log("Skip " + skip);
    }
  }

  private static boolean extract(File archiveFile, String destinationDirectoryPath,
                                 String name, int sequence)
      throws Exception
  {
    File characterDirectory = new File(destinationDirectoryPath + "\\" + name + "\\" +
                                       String.format("%02d", sequence));
    Files.createDirectories(Paths.get(characterDirectory.getAbsolutePath()));
    log("Extract " + archiveFile.getAbsolutePath() + " to " +
        characterDirectory.getAbsolutePath());
    boolean hasPhys = false;
    SevenZFile sevenZFile = new SevenZFile(archiveFile);
    SevenZArchiveEntry sevenZArchiveEntry = sevenZFile.getNextEntry();
    while (sevenZArchiveEntry != null)
    {
      if (sequence == 1 || !sevenZArchiveEntry.getName().toUpperCase().endsWith(".---C"))
      {
        log(sevenZArchiveEntry.getName());
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

  private static void log(String message)
  {
    System.out.println(new Date() + "\t" + message);
  }
}
