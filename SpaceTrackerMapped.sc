SpaceLinemapMapped : SpaceLinemap {

  var <>bufspec;

  *new {
    arg naming, bufspec;
    ^super.new(naming).bufspec_(bufspec).init;
  }

  mapSymbolic {
    arg line;
    line = super.mapSymbolic(line);
    ^line;
  }

  mapNumeric {
    arg line;
    line = super.mapNumeric(line);
    ^line;
  }

/*
    bufspec.do {|s, i|
      var base = path.dirname +/+ PathName(path).fileNameWithoutExtension;
      base.mkdir;
      s[1..].pairsDo { |j, b|
        var p = base +/+ i ++ $- ++ j ++ ".wav";
        b.write(p, "wav", "float");
      };
    };
*/

}

+ SpaceTracker {
  *toMapped {
    arg server, treefile;
    ^super.new.init(treefile).toMapped(server);
  }
  base {
    ^tree.path.dirname +/+ PathName(tree.path).fileNameWithoutExtension;
  }
  meta {
    arg base;
    ^ base +/+ "meta";
  }
  loadChannels {
    arg meta;
    ^File.use(meta, "r") {|f| f.readAllString.split($ ).collect{|ch|ch.asInteger}};
  }
  mappedTmp {
  }

  toMappedCached {
    arg server;
    var base, bufspec, channels;
    base = this.base;
    channels = this.loadChannels(this.meta(base)); 
    bufspec = this.readSoundFilesCollect { |file, i|
      var buf, snd, bufspecch;
      buf = Buffer.read(server, file);
      buf.path = file;
      SoundFile.use(file,  {|snd|
        buf.numChannels = snd.numChannels;
      });
      //"cont2".postln;
      File.exists(file++".mapped.2");
      bufspecch = [buf] ++ channels.collect { |ch| [ch, Buffer.read(server, file ++ ".mapped."++ch)] }.flat;
    };
    polyphony = bufspec[0].size;
    ^bufspec; 
  }

  *fromMapped {
    arg treefile, bufspec, frames;
    ^super.new.init(treefile).fromMapped(bufspec, frames);
  }
  isMapped {
    ^PathName(this.base).isFolder;
  }
}

