package nl.topicus.plugins.maven.i2a;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Create a "translation" of a Wicket .properties file, replacing words in property values
 * by random words from a dictionary. Parameters like ${name} and {0} are preserved.  
 * 
 * @author Sander Evers
 * 
 */
@Mojo(name = "translate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class I2A extends AbstractMojo
{

	@Parameter
	private String dictionary;

	@Parameter(required = true)
	private String source;
	
	@Parameter(required = true)
	private String target;
	
//	@Parameter(property="basedir")
//	private String projectBasedir;
		
	@Component
	private BuildContext buildContext;
	
	protected static Random random = new Random();

	// Match word sequences (with words consisting of alphabetic letters), these will be translated.
	// Also match "{...}" parameters, these will be skipped.
	protected static Pattern phrasePattern = Pattern.compile("(\\{.*?\\})|([\\s\\p{IsAlphabetic}]+)", Pattern.UNICODE_CHARACTER_CLASS);
	
	private static final String DEFAULT_DICT_FILE = "names.txt";

	protected Reader createDictionaryFileReader() throws FileNotFoundException, UnsupportedEncodingException {
		if (dictionary != null)
			return new FileReader(dictionary);
		else
			return new InputStreamReader(I2A.class.getResourceAsStream(DEFAULT_DICT_FILE), "UTF-8");
	}
	
	protected List<String> readDictionary() throws IOException
	{
		Reader reader = createDictionaryFileReader();
		StreamTokenizer tok = new StreamTokenizer(reader);
		tok.wordChars(' ', ' ');
		tok.wordChars('\'', '\'');
		List<String> ret = new ArrayList<>();
		int nt;
		while ((nt = tok.nextToken()) != StreamTokenizer.TT_EOF)
			if (nt != StreamTokenizer.TT_EOL)
				ret.add(tok.sval);
		reader.close();
		return ret;
	}

	protected String translateWords(String words, List<String> dictionary)
	{
		String[] oldWords = (words).split(" ");
		StringBuilder newWords = new StringBuilder();
		int pos = 0;
		while (pos < oldWords.length)
		{
			if (oldWords[pos].equals(""))
			{
				if (pos > 0)
					newWords.append(" ");
				pos++;
			}
			else
			{
				String candidate = dictionary.get(random.nextInt(dictionary.size()));
				if (candidate.split(" ").length + pos <= oldWords.length)
				{
					if (pos > 0)
						newWords.append(" ");
					newWords.append(candidate);
					pos += candidate.split(" ").length;
				}
			}
		}
		if (words.endsWith(" "))
			newWords.append(" ");
		// System.out.println(oldWords.length +":"+ words+"=>"+newWords.toString()+"<=");
		return newWords.toString();
	}

	protected String translatePhrase(String phrase, List<String> dictionary)
	{
		Matcher matcher = phrasePattern.matcher(phrase);
		StringBuilder sb = new StringBuilder();

		int last = 0;
		while (matcher.find())
		{
			sb.append(phrase, last, matcher.start());
			if (matcher.group(2) != null)
			{
				sb.append(translateWords(matcher.group(2), dictionary));
				last = matcher.end();
			}
			else
			{
				last = matcher.start();
			}
		}
		sb.append(phrase, last, phrase.length());
		return sb.toString();
	}

	@Override
	public void execute() throws MojoExecutionException
	{		
		Properties propsNL = new Properties();
		try {
			File sourceFile = new File(source);
			if (!buildContext.hasDelta(sourceFile))
				return;	
			getLog().info("source: "+source);
			Reader reader =	new InputStreamReader(new FileInputStream(sourceFile), "ISO-8859-1");
			propsNL.load(reader);
			reader.close();
		} catch (IOException e) {
			throw new MojoExecutionException("Error reading source file: "+source, e);
		}
		
		List<String> dictionary;
		try {
			dictionary = readDictionary();
		} catch (IOException e) {
			throw new MojoExecutionException("Error reading dictionary file: "+source, e);
		}	

		Properties propsSV = new Properties()
		{
			// hack to store sorted, see
			// http://stackoverflow.com/questions/54295/how-to-write-java-util-properties-to-xml-with-sorted-keys

			private static final long serialVersionUID = 1L;

			@Override
			public Set<Object> keySet()
			{
				return Collections.unmodifiableSet(new TreeSet<Object>(super.keySet()));
			}

			@Override
			public synchronized Enumeration<Object> keys()
			{
				return Collections.enumeration(new TreeSet<Object>(super.keySet()));
			}
		};
		for (String key : propsNL.stringPropertyNames())
		{
			propsSV.put(key, translatePhrase(propsNL.getProperty(key), dictionary));
		}

		
		try {
			File targetFile = new File(target);
			OutputStream os = buildContext.newFileOutputStream(targetFile);
			if (target.toLowerCase().endsWith(".xml"))
				propsSV.storeToXML(os, "");
			else {
				Writer writer = new OutputStreamWriter(os, "UTF-8");
				propsSV.store(writer,"");	
			}
			os.close();
		} catch (IOException e) {
			throw new MojoExecutionException("Error writing target file: "+target, e);
		}	

		getLog().info("Output stored in " + target);


	}

}
