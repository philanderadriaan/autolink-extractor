import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;

public class Main
{
  private static final Random RANDOM = new Random();

  public static void main(String[] args) throws Exception
  {
//    
//    Map<String, List<List<String>>> audioDirectoryPathMap =
//        new Gson().fromJson(Files.newBufferedReader(Paths.get("audio.json")), Map.class);
//    List<String> audioKeyList = new ArrayList<String>(audioDirectoryPathMap.keySet());
//    String audioKey = audioKeyList.get(RANDOM.nextInt(audioKeyList.size()));
//
//    Map<Integer, List<File>> audioFileMap = new HashMap<Integer, List<File>>();
//    for (int i = 0; i < audioDirectoryPathMap.get(audioKey).size(); i++)
//    {
//      if (!audioFileMap.containsKey(i))
//      {
//        audioFileMap.put(i, new ArrayList<File>());
//      }
//      for (String audioDirectoryPath : audioDirectoryPathMap.get(audioKey).get(i))
//      {
//        for (File audioFile : new File(audioDirectoryPath).listFiles())
//        {
//          audioFileMap.get(i).add(audioFile);
//        }
//      }
//      Collections.shuffle(audioFileMap.get(i));
//    }
//
//    List<File> playlist = new ArrayList<File>();
//    playlist.add(audioFileMap.get(1).remove(0));
//    playlist.add(null);
//
//    int streak = 1;
//    while (!audioFileMap.get(0).isEmpty() && RANDOM.nextDouble() < 1 / Math.log(streak))
//    {
//      playlist.add(audioFileMap.get(0).remove(0));
//      System.out.println(Math.log(streak));
//      streak++;
//    }
//    AudioUtility.playBackground(playlist.toArray(new File[playlist.size()]));

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
          FileUtils.deleteDirectory(characterDirectory);
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
                                 String name, int costume)
      throws Exception
  {
    File characterDirectory = new File(destinationDirectoryPath + "\\" + name + "\\" +
                                       String.format("%02d", costume));
    Files.createDirectories(Paths.get(characterDirectory.getAbsolutePath()));
    LogUtility.log("Extract " + archiveFile.getAbsolutePath() + " to " +
                   characterDirectory.getAbsolutePath());
    boolean hasPhys = false;
    SevenZFile sevenZFile = new SevenZFile(archiveFile);
    SevenZArchiveEntry sevenZArchiveEntry = sevenZFile.getNextEntry();
    while (sevenZArchiveEntry != null)
    {
      if (!sevenZArchiveEntry.isDirectory() &&
          (costume == 1 ||
           !FileNameUtils.getExtension(sevenZArchiveEntry.getName()).equalsIgnoreCase("---C")))
      {
        LogUtility.log(sevenZArchiveEntry.getName());
        FileOutputStream fileOutputStream =
            new FileOutputStream(characterDirectory + "\\" + sevenZArchiveEntry.getName()
                .substring(sevenZArchiveEntry.getName().lastIndexOf('/') + 1));
        byte[] bytes = new byte[(int) sevenZArchiveEntry.getSize()];
        sevenZFile.read(bytes, 0, bytes.length);
        fileOutputStream.write(bytes);
        fileOutputStream.close();
        if (FileNameUtils.getExtension(sevenZArchiveEntry.getName()).equalsIgnoreCase("PHYD"))
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
