package webserver;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FileSystemCache
{
    private final String contentRoot;
    private final boolean enabled;
    private final Map<String, String> cachedContent;
    
    public FileSystemCache(String contentRoot, boolean enabled)
    {
        this.contentRoot = contentRoot;
        this.enabled = enabled;
        this.cachedContent = new HashMap<String, String>();
    }
    
    public String getFileFromRelativePath(String path) throws Exception
    {
        if (enabled)
        {
            if (! this.cachedContent.containsKey(path))
            {
                this.cachedContent.put(path, this.readFileFromRelativePath(path));
            }

            return this.cachedContent.get(path);
        }
        else
        {
            return this.readFileFromRelativePath(path);
        }
    }
    
    private String readFileFromRelativePath(String path) throws Exception
    {
        StringBuilder content = new StringBuilder();
        
        try(BufferedReader reader = new BufferedReader(new FileReader(this.contentRoot + "\\" + path)))
        {
            String line = reader.readLine();
            
            while (line != null)
            {
                content.append(line);
                content.append(System.lineSeparator());
                line = reader.readLine();
            }
            
            return content.toString();
        }
    }
}
