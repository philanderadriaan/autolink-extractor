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
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;

public class Main
{
  public static void main(String[] args) throws Exception
  {
    Properties prop = new Properties();
    prop.load(new FileInputStream("cfg.properties"));
    File srcDir = new File(prop.getProperty("src"));
    File destDir = new File(prop.getProperty("dest"));

    Set<String> exSet = new HashSet<String>();
    Set<String> skipSet = new HashSet<String>();
    for (File srcFile : srcDir.listFiles())
    {
      skipSet.add(srcFile.getName());
    }

    List<String> nameList =
        Files.readAllLines(Paths.get(destDir.getAbsolutePath() + "\\NAME.txt"));
    Collections.shuffle(nameList);

    for (String name : nameList)
    {
      for (File charDir : destDir.listFiles())
      {
        if (charDir.isDirectory() && charDir.getName().equals(name))
        {
          LogUtil.out("Delete " + charDir.getAbsolutePath());
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
            exSet.add(e.getMessage());
            e.printStackTrace();
          }
          skipSet.remove(archiveFile.getName());
        }
      }
    }
    for (String skip : skipSet)
    {
      LogUtil.out("Skip " + skip);
    }
    for (String ex : exSet)
    {
      LogUtil.err(ex);
    }
    WavUtil.flush();
  }

  private static boolean extract(File archiveFile, String destDirPath,
                                 String name, int costume)
      throws Exception
  {
    File charDir = new File(destDirPath + "\\" + name + "\\" +
                                       String.format("%02d", costume));
    Files.createDirectories(Paths.get(charDir.getAbsolutePath()));
    LogUtil.out("Extract " + archiveFile.getAbsolutePath() + " to " +
                   charDir.getAbsolutePath());
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
        FileOutputStream stream =
            new FileOutputStream(charDir + "\\" + zEntry.getName()
                .substring(zEntry.getName().lastIndexOf('/') + 1));
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
