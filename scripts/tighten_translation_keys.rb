#!/usr/bin/env ruby
require "digest"
require "set"
ROOT=File.expand_path("..",__dir__)
RES=File.join(ROOT,"src/main/resources")
migration_path=File.join(RES,"db/migration/V26__short_translation_keys.sql")
migration=File.read(migration_path,encoding:"UTF-8")
pairs=migration.scan(/\('([^']+)','([^']+)'\)/)
used=Set.new
final={}
pairs.each do |_old,current|
  scope,slug=current.split(".",2);max_slug=40-scope.length-1
  candidate="#{scope}.#{slug[0,max_slug].sub(/_+\z/,"")}"
  if used.include?(candidate)
    suffix=Digest::SHA1.hexdigest(current)[0,4]
    candidate="#{scope}.#{slug[0,max_slug-5].sub(/_+\z/,"")}_#{suffix}"
  end
  used<<candidate;final[current]=candidate
end
Dir[File.join(RES,"templates/**/*.html")].each do |path|
  source=File.read(path,encoding:"UTF-8");changed=source.dup
  final.each{|old,new_key|changed.gsub!(old,new_key)}
  File.write(path,changed,encoding:"UTF-8") if changed!=source
end
%w[messages.properties messages_az.properties messages_en.properties messages_ru.properties].each do |name|
  path=File.join(RES,name);lines=File.readlines(path,encoding:"UTF-8")
  File.write(path,lines.map{|line|key,value=line.chomp.split("=",2);value&&final[key] ? "#{final[key]}=#{value}\n" : line}.join,encoding:"UTF-8")
end
final.each{|old,new_key|migration.gsub!(",'#{old.gsub("'","''")}')",",'#{new_key.gsub("'","''")}')")}
File.write(migration_path,migration,encoding:"UTF-8")
puts "Açar standartının maksimum uzunluğu #{final.values.map(&:length).max} simvola endirildi."
