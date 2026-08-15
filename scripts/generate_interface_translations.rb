#!/usr/bin/env ruby
require "json"
require "net/http"
require "uri"

ROOT=File.expand_path("..",__dir__)
SOURCE=File.join(ROOT,"src/main/resources/messages_az.properties")
SEPARATOR="<<<I18N_SPLIT_7F3A>>>"
TARGETS={"en"=>"messages_en.properties","ru"=>"messages_ru.properties"}.freeze

def read_properties(path)
  File.readlines(path,encoding:"UTF-8").each_with_object({}) do |line,result|
    next if line.strip.empty?||line.start_with?("#","!")
    key,value=line.chomp.split("=",2);result[key]=value if value
  end
end

def translate(values,target)
  uri=URI("https://translate.googleapis.com/translate_a/single")
  response=Net::HTTP.post_form(uri,{"client"=>"gtx","sl"=>"az","tl"=>target,"dt"=>"t","q"=>values.join("\n#{SEPARATOR}\n")})
  raise "HTTP #{response.code}" unless response.is_a?(Net::HTTPSuccess)
  text=JSON.parse(response.body).first.map{|part|part.first}.join
  parts=text.split(/\s*#{Regexp.escape(SEPARATOR)}\s*/, -1).map(&:strip)
  raise "Gözlənilən #{values.size}, alınan #{parts.size}" unless parts.size==values.size
  parts
end

source=read_properties(SOURCE)
TARGETS.each do |locale,filename|
  target_path=File.join(ROOT,"src/main/resources",filename)
  existing=File.exist?(target_path) ? read_properties(target_path) : {}
  pending=source.reject{|key,_|existing.key?(key)}.to_a
  pending.each_slice(20).with_index(1) do |batch,index|
    attempts=0
    begin
      translated=translate(batch.map(&:last),locale)
      batch.zip(translated).each{|(key,_),value|existing[key]=value.gsub("\n","\\n")}
      puts "#{locale}: #{[index*20,pending.size].min}/#{pending.size}";STDOUT.flush
      sleep 0.15
    rescue StandardError=>e
      attempts+=1;raise if attempts>=5
      warn "#{locale}: təkrar cəhd #{attempts} (#{e.message})";sleep attempts*2;retry
    end
  end
  ordered=source.keys.map{|key|"#{key}=#{existing.fetch(key)}"}.join("\n")+"\n"
  File.write(target_path,ordered,encoding:"UTF-8")
end
