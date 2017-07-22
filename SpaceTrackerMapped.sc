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

  toMapped {
    arg server;
    var base, bufspec, channels, buffers;
    base = this.base;
    channels = this.loadChannels(this.meta(base)); 
    
    this.toSoundFile;
    bufspec=sounds.collect {|read|
      var write, line, spec, writeposch;
      read.openRead;
      write = SoundFile(read.path ++ ".ren");
      write.numChannels = numChannels;
      write.sampleFormat = sampleFormat;
      write.headerFormat = headerFormat;
      write.openWrite;

      line = FloatArray.newClear(read.numChannels);
      spec = channels.collect {|ch|
        var writech;
        writech = SoundFile(read.path ++ ".mapped."++ch);
        writech.headerFormat = headerFormat;
        writech.sampleFormat = sampleFormat;
        writech.openWrite;
        [ch, writech];
      }.flatten;
      writeposch = Array.fill(channels.maxItem+1, 0);
      while { read.readData(line); line.size > 0 }{
        var count, path, frames;
        spec.pairsDo { |ch, writech, i|
          count = line[ch+1];

          path = (base +/+ count ++ $.++$*).pathMatch.first;

          line[ch+1] = writeposch[ch];

          if (path.notNil) {
            SoundFile.use(path) { |readch|
              var chunk;
              frames = (line[0] * server.sampleRate/server.options.blockSize).ceil.max(readch.numFrames);
              chunk = FloatArray.newClear(16384);
              while { chunk.size > 0 }{
                readch.readData(chunk);
                writech.writeData(chunk);
              };
              writech.writeData(FloatArray.newClear(frames-readch.numFrames));
              writeposch.atInc(ch, frames);
              //[\frames, frames, frames - readch.numFrames, readch.numFrames].postln;
            };
          };
        };
        write.writeData(line);
      };
      read.close;
      write.close;
      "mv % %".format(write.path, read.path).systemCmd;
      spec.pairsDo {|ch, writech, i|
        writech.close;
        spec[i+1] = Buffer.read(server, writech.path);
      };
    };
    buffers = this.toBuffer(server);
    bufspec = bufspec.collect { |bufspecch, i| [buffers[i]]++bufspecch;};
    ^bufspec;
  }
  *fromMapped {
    arg treefile, bufspec, frames;
    ^super.new.init(treefile).fromMapped(bufspec, frames);
  }
  fromMapped {
    arg bufspec, argFrames;
    var buffer, controlrate, base, count,server;
    frames = argFrames;
    server=bufspec[0][0].server;

    controlrate = server.sampleRate / server.options.blockSize;

    base = this.base;

    count = 1;

    forkIfNeeded {
      this.fromBufferInit(bufspec.collect {|bufspecch|bufspecch[0]});
      //frames.postln;
      server.sync;
      if (frames.asArray.every({|e|e==1})) {
        "No frames, not saving %".format(tree.path).warn;
        this.yield;
      };
      base.mkdir;
      bufspec.do { |bufspecch, i|
        var read, write, line;
        bufspecch[1..].pairsDo {|ch, b|
          var read, write, line, path, frames, start, file;
          read = SoundFile(this.soundFileName(i));
          read.openRead;
          write = SoundFile(tmp.file(soundExtension));
          write.numChannels = read.numChannels;
          write.headerFormat = read.headerFormat;
          write.sampleFormat = read.sampleFormat;
          write.openWrite;
          line = FloatArray.newClear(read.numChannels);
          while { read.readData(line); line.size > 0 } {
            start = line[ch+1]; // ch is output channel index, buffers have additional time channel at beginning, so ch+1
            frames = controlrate * line[0];
            path = base +/+ count ++ $. ++ soundExtension;
            b.write(path, headerFormat, sampleFormat, frames.asInteger, start.asInteger);
            line[ch+1] = count;
            write.writeData(line);
            count=count+1;
            b.server.sync; // Without this, longer passages crash the server
          };
          read.close;
          write.close;
          "mv % %".format(write.path, read.path).systemCmd;
        };
      };
      //PathName(base).filesDo {|path|
      //  if (path.extension == soundExtension) {
      //    SoundFile.s_convert(path.fullPath, controlrate);
      //  };
      //};
      server.sync;
      this.writeTree;
      File.use(this.meta(base), "w", {|f|
        var channels;
        channels = bufspec[0][1..].clump(2).collect {|c|c[0]};
        f.write(channels.join(" "));
      });
    }
  }
  isMapped {
    ^PathName(this.base).isFolder;
  }
}

