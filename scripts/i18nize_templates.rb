#!/usr/bin/env ruby
# Existing Thymeleaf templates üçün birdəfəlik mexaniki i18n keçidi.
require "digest"

ROOT = File.expand_path("..", __dir__)
ALL_FILES = Dir[File.join(ROOT, "src/main/resources/templates/**/*.html")]
requested = ENV.fetch("I18N_FILES", "").split(",").map(&:strip).reject(&:empty?)
FILES = requested.empty? ? ALL_FILES : ALL_FILES.select { |path| requested.include?(File.basename(path)) }
PROPERTIES = %w[messages.properties messages_az.properties].map { |f| File.join(ROOT, "src/main/resources", f) }
TRANSLITERATION = {"ə"=>"e","Ə"=>"e","ı"=>"i","İ"=>"i","ö"=>"o","Ö"=>"o","ü"=>"u","Ü"=>"u","ş"=>"s","Ş"=>"s","ç"=>"c","Ç"=>"c","ğ"=>"g","Ğ"=>"g"}.freeze
LEAF_TAGS = %w[title h1 h2 h3 h4 h5 h6 p span small label button option a th td dt dd textarea div strong b].join("|")
ATTRIBUTES = %w[placeholder title aria-label].freeze

def meaningful?(text)
  value=text.strip
  !value.empty? && value.match?(/[[:alpha:]]/) && !value.include?('#{') && !value.include?('${') && !value.include?('@{')
end

PREFIXES={"shobe.html"=>"departments","xidmet.html"=>"services","teskilatlar.html"=>"organizations","xetaJurnali.html"=>"error_log"}.freeze
def key_for(text,prefix)
  normalized=text.strip.gsub(/\s+/," ")
  slug=normalized.chars.map { |c| TRANSLITERATION.fetch(c,c) }.join.downcase
      .gsub(/[^a-z0-9]+/,"_").gsub(/^_|_$/,"")[0,48]
  slug="text" if slug.empty?
  max=[40-prefix.length-1,12].max
  "#{prefix}.#{slug[0,max].sub(/_+$/,'')}"
end

catalog={}
FILES.each do |path|
  prefix=PREFIXES.fetch(File.basename(path),"common")
  source=File.read(path,encoding:"UTF-8")
  changed=source.gsub(/<(#{LEAF_TAGS})(\s[^<>]*?)?>([^<>]+)<\/\1>/im) do |match|
    tag=$1;attrs=$2.to_s;text=$3
    next match if attrs.match?(/\bth:text\s*=/i) || !meaningful?(text)
    clean=text.strip.gsub(/\s+/," ");key=key_for(clean,prefix);catalog[key]=clean
    "<#{tag}#{attrs} th:text=\"\#{#{key}}\">#{text}</#{tag}>"
  end
  changed=changed.gsub(/(<i\b[^>]*><\/i>)([^<>]+)(<\/(?:button|a|h[1-6]|span|div|p|small|label|th|td)>)/im) do |match|
    icon=$1;text=$2;closing=$3
    next match unless meaningful?(text)
    clean=text.strip.gsub(/\s+/," ");key=key_for(clean,prefix);catalog[key]=clean
    "#{icon}<span th:text=\"\#{#{key}}\">#{text}</span>#{closing}"
  end
  changed=changed.gsub(/(<(?:h[1-6]|p|span|small|label|button|a|th|td|div|strong|b)\b(?![^>]*\bth:text=)[^>]*>)([^<>]+)(?=<)/im) do |match|
    opening=$1;text=$2;next match unless meaningful?(text)
    clean=text.strip.gsub(/\s+/," ");key=key_for(clean,prefix);catalog[key]=clean
    "#{opening}<span th:text=\"\#{#{key}}\">#{text}</span>"
  end
  changed=changed.gsub(/(<\/(?:i|span|small|strong|b|em|div|a)>|<br\s*\/?>)([^<>]+)(?=<)/im) do |match|
    closing=$1;text=$2;next match unless meaningful?(text)
    clean=text.strip.gsub(/\s+/," ");key=key_for(clean,prefix);catalog[key]=clean
    "#{closing}<span th:text=\"\#{#{key}}\">#{text}</span>"
  end
  ATTRIBUTES.each do |attribute|
    source=changed
    changed=source.gsub(/<([a-z][\w:-]*)([^<>]*?\s#{Regexp.escape(attribute)}="([^"]+)"[^<>]*?)>/im) do |match|
      tag=$1;attrs=$2;text=$3
      next match if attrs.match?(/\bth:#{Regexp.escape(attribute)}\s*=/i) || !meaningful?(text)
      key=key_for(text,prefix);catalog[key]=text.strip.gsub(/\s+/," ")
      "<#{tag}#{attrs} th:#{attribute}=\"\#{#{key}}\">"
    end
  end
  changed=changed.gsub(/<([a-z][\w:-]*)([^<>]*?\sdata-placeholder="([^"]+)"[^<>]*?)>/im) do |match|
    tag=$1;attrs=$2;text=$3
    next match if attrs.match?(/\bth:attr\s*=.*data-placeholder/i) || !meaningful?(text)
    key=key_for(text,prefix);catalog[key]=text.strip.gsub(/\s+/," ")
    "<#{tag}#{attrs} th:attr=\"data-placeholder=\#{#{key}}\">"
  end
  changed.scan(/th:text="\#\{(ui\.[^}]+)\}"[^>]*>([^<>]+)</im) do |key,text|
    catalog[key]=text.strip.gsub(/\s+/," ") if meaningful?(text)
  end
  ATTRIBUTES.each do |attribute|
    changed.scan(/#{Regexp.escape(attribute)}="([^"]+)"[^>]*th:#{Regexp.escape(attribute)}="\#\{(ui\.[^}]+)\}"/im) do |text,key|
      catalog[key]=text.strip.gsub(/\s+/," ") if meaningful?(text)
    end
  end
  changed.scan(/data-placeholder="([^"]+)"[^>]*th:attr="data-placeholder=\#\{(ui\.[^}]+)\}"/im) do |text,key|
    catalog[key]=text.strip.gsub(/\s+/," ") if meaningful?(text)
  end
  File.write(path,changed,encoding:"UTF-8") if changed!=File.read(path,encoding:"UTF-8")
end

PROPERTIES.each do |path|
  existing=File.exist?(path) ? File.read(path,encoding:"UTF-8") : ""
  known=existing.lines.map { |line| line[/\A([^#!\s][^=]*)=/,1] }.compact.each_with_object({}) { |key,map| map[key]=true }
  additions=catalog.reject { |key,_| known[key] }.sort.map { |key,value| "#{key}=#{value.gsub("\\","\\\\").gsub("\n","\\n")}" }
  File.write(path,existing.rstrip+"\n"+additions.join("\n")+"\n",encoding:"UTF-8") unless additions.empty?
end

puts "#{FILES.size} şablon yoxlanıldı, #{catalog.size} statik mətn kataloqlaşdırıldı."
