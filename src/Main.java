import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;

public class Main
{
  public static void main(String[] args) throws InterruptedException, LineUnavailableException,
      IOException, UnsupportedAudioFileException
  {
    Properties prop = new Properties();
    prop.load(new FileInputStream("prop.properties"));
    File srcDir = new File(prop.getProperty("src"));
    File destDir = new File(prop.getProperty("dest"));

    Set<String> errSet = new HashSet<String>();
    Set<String> skipSet = new HashSet<String>();
    for (File srcFile : srcDir.listFiles())
    {
      skipSet.add(srcFile.getName());
    }

    List<String> nameList =
        Files.readAllLines(Paths.get(destDir.getAbsolutePath() + "/NAME.txt"));
    Collections.shuffle(nameList);

    for (String name : nameList)
    {
      for (File charDir : destDir.listFiles())
      {
        if (charDir.isDirectory() && charDir.getName().equals(name))
        {
          LogUtil.out("Deleting " + charDir.getAbsolutePath());
          FileUtils.deleteDirectory(charDir);
        }
      }
      for (File archiveFile : srcDir.listFiles())
      {
        if (archiveFile.isFile() &&
            archiveFile.getName().toUpperCase().startsWith(name.substring(0, 3).toUpperCase()))
        {
          try
          {
            if (extract(archiveFile, destDir.getAbsolutePath(), name, 1))
            {
              extract(archiveFile, destDir.getAbsolutePath(), name, 2);
            }
          }
          catch (Exception e)
          {
            errSet.add(e.getMessage());
            e.printStackTrace();
          }
          skipSet.remove(archiveFile.getName());
        }
      }
    }
    for (String skip : skipSet)
    {
      LogUtil.out("Skipped " + skip);
    }
    for (String err : errSet)
    {
      LogUtil.err(err);
    }
    WavUtil.flush();
  }

  private static boolean extract(File archiveFile, String destDirPath, String name,
                                 int costume)
      throws IOException

  {
    File charDir = new File(destDirPath + '/' + name + '/' + String.format("%02d", costume));
    Files.createDirectories(Paths.get(charDir.getAbsolutePath()));
    LogUtil
        .out("Extracting " + archiveFile.getAbsolutePath() + " to " + charDir.getAbsolutePath());
    boolean hasPhys = false;
    SevenZFile zFile = new SevenZFile(archiveFile);
    SevenZArchiveEntry zEntry = zFile.getNextEntry();
    while (zEntry != null)
    {
      if (!zEntry.isDirectory() &&
          (costume == 1 ||
           !FileNameUtils.getExtension(zEntry.getName()).equalsIgnoreCase("---C")))
      {
        LogUtil.out(zEntry.getName());
        FileOutputStream stream = new FileOutputStream(charDir.getAbsolutePath() + '/' + zEntry
            .getName().substring(zEntry.getName().lastIndexOf('/') + 1));
        byte[] bytes = new byte[(int) zEntry.getSize()];
        zFile.read(bytes, 0, bytes.length);
        stream.write(bytes);
        stream.close();
        if (FileNameUtils.getExtension(zEntry.getName()).equalsIgnoreCase("PHYD"))
        {
          hasPhys = true;
        }
      }
      zEntry = zFile.getNextEntry();
    }
    zFile.close();
    return hasPhys;
  }
}
